package com.bing.bingaicode.rag.node;

import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.rag.service.KnowledgeBaseService;
import com.bing.bingaicode.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Step 2: RAG 检索节点（Retriever）
 *
 * 职责：根据 Step 1 解析出的需求关键词，从企业知识库中检索相关的
 * 组件规范、设计系统、编码规范和页面模板。
 */
@Slf4j
public class RAGRetrieverNode {

    /** 默认检索数量 */
    private static final int DEFAULT_TOP_K = 10;

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: RAG 知识库检索 (Retriever)");
            String ragQuery = context.getRagQueryKeywords();
            if (StrUtil.isBlank(ragQuery)) {
                // 没有检索关键词，使用原始 prompt 的前 200 字作为降级查询
                ragQuery = context.getOriginalPrompt();
                if (ragQuery.length() > 200) {
                    ragQuery = ragQuery.substring(0, 200);
                }
                log.info("RAG 降级查询: 使用原始 prompt 前 200 字");
            }
            try {
                KnowledgeBaseService knowledgeBaseService = SpringContextUtil.getBean(KnowledgeBaseService.class);
                // 执行多维度检索
                // 1. 通用语义检索
                List<KnowledgeBaseChunk> generalResults = knowledgeBaseService.search(
                        ragQuery, null, null, DEFAULT_TOP_K);
                // 2. 合规规则全量检索（确保安全规则不遗漏）
                List<KnowledgeBaseChunk> complianceRules = knowledgeBaseService.getChunksByType("compliance_rule");
                // 3. 合并去重
                java.util.Set<Long> seenIds = new java.util.HashSet<>();
                java.util.List<KnowledgeBaseChunk> mergedResults = new java.util.ArrayList<>();
                // 合规规则优先
                for (KnowledgeBaseChunk chunk : complianceRules) {
                    if (seenIds.add(chunk.getId())) {
                        mergedResults.add(chunk);
                    }
                }
                // 再加通用检索结果
                for (KnowledgeBaseChunk chunk : generalResults) {
                    if (seenIds.add(chunk.getId())) {
                        mergedResults.add(chunk);
                    }
                }
                log.info("RAG 检索完成 - 通用: {}, 合规规则: {}, 合并后: {}",
                        generalResults.size(), complianceRules.size(), mergedResults.size());
                // 将检索结果组装为可注入 Prompt 的上下文文本
                String assembledContext = knowledgeBaseService.assembleChunksToContext(mergedResults);
                // 更新上下文
                context.setCurrentStep("RAG 检索");
                context.setRagContext(assembledContext);
                context.setRagChunks(mergedResults);
                context.setRagChunkCount(mergedResults.size());
            } catch (Exception e) {
                log.error("RAG 检索异常: {}", e.getMessage(), e);
                // 检索失败不阻断流程，使用空上下文继续
                context.setCurrentStep("RAG 检索(降级)");
                context.setRagContext("");
                context.setRagChunkCount(0);
            }
            return WorkflowContext.saveContext(context);
        });
    }
}
