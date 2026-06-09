package com.bing.bingaicode.langgraph4j.state;

import com.bing.bingaicode.langgraph4j.model.ImageCollectionPlan;
import com.bing.bingaicode.langgraph4j.model.ImageResource;
import com.bing.bingaicode.langgraph4j.model.QualityResult;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import com.bing.bingaicode.rag.model.CodeReviewResult;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.rag.model.PRDParserResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文 - 存储所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    /**
     * WorkflowContext 在 MessagesState 中的存储key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 图片资源字符串
     */
    private String imageListStr;

    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum generationType;

    /**
     * 生成的代码目录
     */
    private String generatedCodeDir;

    /**
     * 构建成功的目录
     */
    private String buildResultDir;

    /**
     * 质量检查结果
     */
    private QualityResult qualityResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 图片收集计划
     */
    private ImageCollectionPlan imageCollectionPlan;

    /**
     * 并发图片收集的中间结果字段
     */
    private List<ImageResource> contentImages;
    private List<ImageResource> illustrations;
    private List<ImageResource> diagrams;
    private List<ImageResource> logos;

    // ========== RAG 工作流新增字段 ==========

    /**
     * PRD 解析结果（Step 1 输出）
     */
    private PRDParserResult prdParserResult;

    /**
     * PRD 解析结果的 JSON 序列化
     */
    private String prdParserResultJson;

    /**
     * RAG 检索关键词（由 PRD 解析提取）
     */
    private String ragQueryKeywords;

    /**
     * RAG 检索到的企业规范上下文（可直接注入 Prompt）
     */
    private String ragContext;

    /**
     * RAG 检索到的原始分块列表
     */
    private List<KnowledgeBaseChunk> ragChunks;

    /**
     * RAG 检索到的分块数量
     */
    private int ragChunkCount;

    /**
     * 代码审查结果（Step 4 输出）
     */
    private CodeReviewResult codeReviewResult;

    /**
     * 代码审查重试次数（独立计数器，不依赖 currentStep）
     */
    private int reviewRetryCount;

    @Serial
    private static final long serialVersionUID = 1L;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}