package com.bing.bingaicode.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.bing.bingaicode.langgraph4j.model.ImageResource;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import com.bing.bingaicode.rag.model.PRDParserResult;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强工作节点
 */
@Slf4j
public class PromptEnhancerNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 提示词增强");
            // 获取原始提示词和图片列表
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();
            // 获取 RAG 检索到的企业规范上下文
            String ragContext = context.getRagContext();
            int ragChunkCount = context.getRagChunkCount();
            // 获取 PRD 解析结果
            PRDParserResult prdResult = context.getPrdParserResult();
            // 构建增强后的提示词
            StringBuilder enhancedPromptBuilder = new StringBuilder();

            // ① 企业规范上下文（开头，最强注意力位置）
            if (StrUtil.isNotBlank(ragContext)) {
                enhancedPromptBuilder.append("========================================\n");
                enhancedPromptBuilder.append("⚠️ 以下企业规范必须严格遵守，违反任何规则将导致代码审查不通过！\n");
                enhancedPromptBuilder.append("========================================\n\n");
                enhancedPromptBuilder.append(ragContext);
                enhancedPromptBuilder.append("\n========================================\n\n");
                log.info("已注入企业规范上下文，检索分块数: {}", ragChunkCount);
            }

            // ② PRD 结构化解析结果（Planner Agent 的产出，让 Coder 精确知道该生成什么）
            if (prdResult != null) {
                enhancedPromptBuilder.append(formatPRDResult(prdResult));
                enhancedPromptBuilder.append("\n\n");
                log.info("已注入 PRD 结构化解析结果，页面数: {}",
                        prdResult.getPages() != null ? prdResult.getPages().size() : 0);
            }

            // ③ 原始用户需求
            enhancedPromptBuilder.append(originalPrompt);

            // ④ 图片资源
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材资源\n");
                enhancedPromptBuilder.append("请在生成网站使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("：")
                                .append(image.getDescription())
                                .append("（")
                                .append(image.getUrl())
                                .append("）\n");
                    }
                } else {
                    enhancedPromptBuilder.append(imageListStr);
                }
            }
            String enhancedPrompt = enhancedPromptBuilder.toString();
            // 更新状态
            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("提示词增强完成，增强后长度: {} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 将 PRDParserResult 格式化为可读的结构化描述，注入到 Prompt 中
     */
    private static String formatPRDResult(PRDParserResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 结构化页面定义（AI 必须严格按照以下结构生成代码）\n\n");

        if (StrUtil.isNotBlank(result.getProjectName())) {
            sb.append("项目名称: ").append(result.getProjectName()).append("\n");
        }
        if (StrUtil.isNotBlank(result.getDescription())) {
            sb.append("项目描述: ").append(result.getDescription()).append("\n");
        }
        if (StrUtil.isNotBlank(result.getBusinessDomain())) {
            sb.append("业务领域: ").append(result.getBusinessDomain()).append("\n");
        }
        sb.append("\n");

        if (CollUtil.isNotEmpty(result.getPages())) {
            for (int i = 0; i < result.getPages().size(); i++) {
                PRDParserResult.PageSpec page = result.getPages().get(i);
                sb.append("### 页面 ").append(i + 1).append(": ").append(page.getName()).append("\n");
                if (StrUtil.isNotBlank(page.getRoute())) {
                    sb.append("- 路由: `").append(page.getRoute()).append("`\n");
                }
                if (StrUtil.isNotBlank(page.getDescription())) {
                    sb.append("- 描述: ").append(page.getDescription()).append("\n");
                }
                // 组件需求
                if (StrUtil.isNotBlank(page.getComponents())) {
                    sb.append("- 需要的组件:\n");
                    for (String line : page.getComponents().split("\n")) {
                        if (StrUtil.isNotBlank(line.trim())) {
                            sb.append("  - ").append(line.trim()).append("\n");
                        }
                    }
                }
                // 表单字段
                if (StrUtil.isNotBlank(page.getFormFields())) {
                    sb.append("- 表单字段:\n");
                    for (String field : page.getFormFields().split(";")) {
                        String[] parts = field.trim().split(":");
                        if (parts.length >= 3) {
                            sb.append("  - ").append(parts[1]).append(" (").append(parts[0])
                                    .append("), 类型: ").append(parts[2]);
                            if (parts.length > 3 && "true".equals(parts[3])) {
                                sb.append(", **必填**");
                            }
                            sb.append("\n");
                        }
                    }
                }
                // 表格列
                if (StrUtil.isNotBlank(page.getTableColumns())) {
                    sb.append("- 表格列:\n");
                    for (String col : page.getTableColumns().split(";")) {
                        String[] parts = col.trim().split(":");
                        if (parts.length >= 2) {
                            sb.append("  - ").append(parts[1]).append(" (").append(parts[0]).append(")");
                            if (parts.length > 3) sb.append(", 宽度: ").append(parts[3]);
                            sb.append("\n");
                        }
                    }
                }
                // API 接口
                if (StrUtil.isNotBlank(page.getApiEndpoints())) {
                    sb.append("- API 接口:\n");
                    for (String line : page.getApiEndpoints().split("\n")) {
                        if (StrUtil.isNotBlank(line.trim())) {
                            sb.append("  - ").append(line.trim()).append("\n");
                        }
                    }
                }
                // 交互逻辑
                if (StrUtil.isNotBlank(page.getInteractions())) {
                    sb.append("- 交互逻辑:\n");
                    for (String line : page.getInteractions().split("\n")) {
                        if (StrUtil.isNotBlank(line.trim())) {
                            sb.append("  - ").append(line.trim()).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}