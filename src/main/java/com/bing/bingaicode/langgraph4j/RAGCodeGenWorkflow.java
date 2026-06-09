package com.bing.bingaicode.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.langgraph4j.model.QualityResult;
import com.bing.bingaicode.langgraph4j.node.*;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import com.bing.bingaicode.rag.model.CodeReviewResult;
import com.bing.bingaicode.rag.node.CodeReviewerNode;
import com.bing.bingaicode.rag.node.PRDParserNode;
import com.bing.bingaicode.rag.node.RAGRetrieverNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * RAG 增强的代码生成工作流
 *
 * 4 步 Agent 工作流：
 * Step 1: PRD 解析 (Planner) - 将 PRD 解析为结构化 JSON，提取 RAG 关键词
 * Step 2: RAG 检索 (Retriever) - 从企业知识库检索相关的组件规范和设计标准
 * Step 3: 代码生成 (Coder) - 结合企业规范生成前端代码
 * Step 4: 代码审查 (Reviewer) - 验证生成的代码是否符合企业规范
 *
 * 流程图：
 * START -> prd_parser -> rag_retriever -> image_collector -> prompt_enhancer -> router -> code_generator -> code_quality_check -> code_reviewer -> (build / skip_build / fail-loop) -> END
 */
@Slf4j
@Component
public class RAGCodeGenWorkflow {

    /** 代码审查最大重试次数 */
    private static final int MAX_REVIEW_RETRIES = 2;

    /**
     * 创建 RAG 增强工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // === 节点注册 ===
                    // Step 1: PRD 解析
                    .addNode("prd_parser", PRDParserNode.create())
                    // Step 2: RAG 检索
                    .addNode("rag_retriever", RAGRetrieverNode.create())
                    // 图片收集（保留原有节点）
                    .addNode("image_collector", ImageCollectorNode.create())
                    // 提示词增强（已支持 RAG 上下文注入）
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // 路由
                    .addNode("router", RouterNode.create())
                    // Step 3: 代码生成
                    .addNode("code_generator", CodeGeneratorNode.create())
                    // 代码质量检查
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    // Step 4: 代码审查
                    .addNode("code_reviewer", CodeReviewerNode.create())
                    // 项目构建
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // === 边连接 ===
                    // Step 1 -> Step 2
                    .addEdge(START, "prd_parser")
                    .addEdge("prd_parser", "rag_retriever")
                    // Step 2 -> 图片收集 -> 提示词增强 -> 路由 -> 代码生成
                    .addEdge("rag_retriever", "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    // 质检后进入代码审查
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "code_reviewer", "code_reviewer",   // 质检通过，进入代码审查
                                    "fail", "code_generator"            // 质检失败，重新生成
                            ))
                    // 代码审查后根据结果决定下一步
                    .addConditionalEdges("code_reviewer",
                            edge_async(this::routeAfterCodeReview),
                            Map.of(
                                    "build", "project_builder",    // 审查通过且需要构建
                                    "skip_build", END,             // 审查通过但跳过构建
                                    "revise", "code_generator"     // 审查未通过，重新生成
                            ))
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "RAG 工作流创建失败: " + e.getMessage());
        }
    }

    /**
     * 质检后的路由逻辑
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        log.info("代码质检通过，进入代码审查阶段");
        return "code_reviewer";
    }

    /**
     * 代码审查后的路由逻辑
     */
    private String routeAfterCodeReview(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeReviewResult reviewResult = context.getCodeReviewResult();
        // 检查审查重试次数（通过 currentStep 中的计数器）
        int reviewRetryCount = getReviewRetryCount(context);
        if (reviewResult == null || reviewResult.getIsValid() == null) {
            log.info("代码审查结果为空，视为通过");
            return routeBuildOrSkip(state);
        }
        if (reviewResult.getIsValid()) {
            log.info("代码审查通过，得分: {}", reviewResult.getScore());
            return routeBuildOrSkip(state);
        }
        // 审查未通过，但超过重试次数则放行
        if (context.getReviewRetryCount() >= MAX_REVIEW_RETRIES) {
            log.warn("代码审查未通过但已达最大重试次数({})，放行。得分: {}",
                    MAX_REVIEW_RETRIES, reviewResult.getScore());
            return routeBuildOrSkip(state);
        }
        log.warn("代码审查未通过（得分: {}），第 {} 次重试",
                reviewResult.getScore(), context.getReviewRetryCount() + 1);
        // 重建 enhancedPrompt：规范(开头) + 原始需求(中间) + 审查错误(结尾)
        rebuildPromptForRetry(context, reviewResult);
        return "revise";
    }

    /**
     * 为审查重试重建 prompt 结构，对抗 LLM 的 U 型注意力衰减。
     *
     * 结构：[企业规范摘要(开头)] + [原始 PRD + PRD 结构化结果(中间)] + [审查错误(结尾)]
     * - 开头放规范摘要：确保重试时规范不会被忽略
     * - 中间放原始需求：保持需求不被冲淡
     * - 结尾放审查错误：LLM 对结尾的注意力最强，错误会被优先修正
     */
    private void rebuildPromptForRetry(WorkflowContext context, CodeReviewResult reviewResult) {
        StringBuilder rebuilt = new StringBuilder();

        // ① 开头：重复关键规范摘要（对抗注意力衰减）
        String ragContext = context.getRagContext();
        if (ragContext != null && !ragContext.isBlank()) {
            rebuilt.append("========================================\n");
            rebuilt.append("⚠️ 重试提醒：以下企业规范仍然必须严格遵守！\n");
            rebuilt.append("========================================\n\n");
            rebuilt.append(ragContext);
            rebuilt.append("\n========================================\n\n");
        }

        // ② 中间：原始 PRD 需求 + 结构化页面定义（去掉上一次的错误追加）
        // 从原始 enhancedPrompt 中截取规范之后、错误之前的干净部分
        String cleanPrompt = extractCleanPrompt(context);
        rebuilt.append(cleanPrompt);

        // ③ 结尾：本次审查错误（LLM 对结尾注意力最强）
        rebuilt.append("\n\n## ⚠️ 代码审查未通过，请严格根据以下问题修复（第 ")
                .append(context.getReviewRetryCount() + 1)
                .append(" 次重试）：\n\n");
        if (reviewResult.getErrors() != null) {
            for (CodeReviewResult.ReviewError error : reviewResult.getErrors()) {
                rebuilt.append("- **[").append(error.getSeverity().toUpperCase()).append("]** ")
                        .append(error.getDescription());
                if (error.getFixSuggestion() != null) {
                    rebuilt.append("\n  → 修复: ").append(error.getFixSuggestion());
                }
                rebuilt.append("\n");
            }
        }
        if (reviewResult.getSuggestions() != null && !reviewResult.getSuggestions().isEmpty()) {
            rebuilt.append("\n### 改进建议：\n");
            for (String suggestion : reviewResult.getSuggestions()) {
                rebuilt.append("- ").append(suggestion).append("\n");
            }
        }
        // 更新计数器和 prompt
        context.setReviewRetryCount(context.getReviewRetryCount() + 1);
        context.setCurrentStep("代码审查重试");
        context.setEnhancedPrompt(rebuilt.toString());
    }

    /**
     * 从 enhancedPrompt 中提取干净的原始内容（去掉之前追加的审查错误段落）
     */
    private String extractCleanPrompt(WorkflowContext context) {
        String prompt = context.getEnhancedPrompt();
        // 查找并移除所有 "## 代码审查未通过" 或 "## ⚠️ 代码审查未通过" 段落
        int errorIdx = prompt.indexOf("## 代码审查未通过");
        if (errorIdx < 0) {
            errorIdx = prompt.indexOf("## ⚠️ 代码审查未通过");
        }
        if (errorIdx >= 0) {
            return prompt.substring(0, errorIdx).trim();
        }
        return prompt;
    }

    /**
     * 根据代码生成类型决定是否需要构建
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        return "build";
    }

    /**
     * 执行 RAG 增强工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("RAG 工作流图:\n{}", graph.content());
        log.info("开始执行 RAG 增强代码生成工作流");
        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- RAG 工作流第 {} 步完成 ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤: {}", currentContext.getCurrentStep());
            }
            stepCounter++;
        }
        log.info("RAG 增强代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 执行 RAG 增强工作流（Flux 流式输出版本）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt) {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                try {
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .build();
                    sink.next(formatSseEvent("workflow_start", Map.of(
                            "message", "开始执行 RAG 增强代码生成工作流",
                            "originalPrompt", originalPrompt
                    )));
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("RAG 工作流图:\n{}", graph.content());
                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- RAG 工作流第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            sink.next(formatSseEvent("step_completed", Map.of(
                                    "stepNumber", stepCounter,
                                    "currentStep", currentContext.getCurrentStep(),
                                    "ragChunkCount", currentContext.getRagChunkCount(),
                                    "reviewScore", currentContext.getCodeReviewResult() != null ?
                                            currentContext.getCodeReviewResult().getScore() : null
                            )));
                        }
                        stepCounter++;
                    }
                    sink.next(formatSseEvent("workflow_completed", Map.of(
                            "message", "RAG 增强代码生成工作流执行完成！"
                    )));
                    sink.complete();
                } catch (Exception e) {
                    log.error("RAG 工作流执行失败: {}", e.getMessage(), e);
                    sink.next(formatSseEvent("workflow_error", Map.of(
                            "error", e.getMessage(),
                            "message", "工作流执行失败"
                    )));
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 执行 RAG 增强工作流（SSE 流式输出版本）
     */
    public SseEmitter executeWorkflowWithSse(String originalPrompt) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Thread.startVirtualThread(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                WorkflowContext initialContext = WorkflowContext.builder()
                        .originalPrompt(originalPrompt)
                        .currentStep("初始化")
                        .build();
                sendSseEvent(emitter, "workflow_start", Map.of(
                        "message", "开始执行 RAG 增强代码生成工作流",
                        "originalPrompt", originalPrompt
                ));
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("RAG 工作流图:\n{}", graph.content());
                int stepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- RAG 工作流第 {} 步完成 ---", stepCounter);
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext != null) {
                        sendSseEvent(emitter, "step_completed", Map.of(
                                "stepNumber", stepCounter,
                                "currentStep", currentContext.getCurrentStep(),
                                "ragChunkCount", currentContext.getRagChunkCount(),
                                "reviewScore", currentContext.getCodeReviewResult() != null ?
                                        currentContext.getCodeReviewResult().getScore() : null
                        ));
                    }
                    stepCounter++;
                }
                sendSseEvent(emitter, "workflow_completed", Map.of(
                        "message", "RAG 增强代码生成工作流执行完成！"
                ));
                emitter.complete();
            } catch (Exception e) {
                log.error("RAG 工作流执行失败: {}", e.getMessage(), e);
                sendSseEvent(emitter, "workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "工作流执行失败"
                ));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventType).data(data));
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: {}", e.getMessage(), e);
        }
    }
}
