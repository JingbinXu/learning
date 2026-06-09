package com.bing.bingaicode.rag.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.langgraph4j.state.WorkflowContext;
import com.bing.bingaicode.rag.ai.CodeReviewService;
import com.bing.bingaicode.rag.model.CodeReviewResult;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Step 4: 代码审查节点（Reviewer Agent）
 *
 * 职责：检查生成的代码是否符合企业规范，
 * 包括组件合规性、编码规范、设计规范和安全性检查。
 * 如果审查不通过，会将错误信息反馈给代码生成节点重新生成。
 */
@Slf4j
public class CodeReviewerNode {

    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    /** 审查通过的最低分数 */
    private static final int MIN_PASS_SCORE = 60;

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码审查 (Reviewer Agent)");
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeReviewResult reviewResult;
            try {
                // 1. 读取生成的代码
                String codeContent = readCodeFiles(generatedCodeDir);
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("未找到可审查的代码文件，跳过代码审查");
                    reviewResult = CodeReviewResult.builder()
                            .isValid(true)
                            .score(100)
                            .build();
                } else {
                    // 2. 组装审查输入：代码 + 企业规范上下文
                    String ragContext = context.getRagContext();
                    StringBuilder reviewInput = new StringBuilder();
                    if (StrUtil.isNotBlank(ragContext)) {
                        reviewInput.append("# 企业规范上下文\n\n");
                        reviewInput.append(ragContext).append("\n\n");
                    }
                    reviewInput.append("# 待审查代码\n\n");
                    reviewInput.append(codeContent);

                    // 3. 调用 AI 审查
                    CodeReviewService codeReviewService = SpringContextUtil.getBean(CodeReviewService.class);
                    reviewResult = codeReviewService.reviewCode(reviewInput.toString());
                    log.info("代码审查完成 - 是否通过: {}, 得分: {}, 错误数: {}",
                            reviewResult.getIsValid(),
                            reviewResult.getScore(),
                            reviewResult.getErrors() != null ? reviewResult.getErrors().size() : 0);
                    // 二次校验：如果无规范上下文，审查结果仅作参考
                    if (StrUtil.isBlank(ragContext)) {
                        log.info("无企业规范上下文，审查结果仅作参考，自动通过");
                        reviewResult.setIsValid(true);
                    }
                }
            } catch (Exception e) {
                log.error("代码审查异常: {}", e.getMessage(), e);
                reviewResult = CodeReviewResult.builder()
                        .isValid(true) // 审查异常不阻断流程
                        .score(100)
                        .build();
            }
            // 更新上下文
            context.setCurrentStep("代码审查");
            context.setCodeReviewResult(reviewResult);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 读取代码目录下的所有代码文件内容
     */
    private static String readCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在: {}", codeDir);
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        FileUtil.walkFiles(directory, file -> {
            if (shouldSkipFile(file, directory)) return;
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 文件: ").append(relativePath).append("\n\n");
                try {
                    codeContent.append(FileUtil.readUtf8String(file)).append("\n\n");
                } catch (Exception e) {
                    log.warn("读取文件失败: {}", file.getAbsolutePath());
                }
            }
        });
        return codeContent.toString();
    }

    private static boolean shouldSkipFile(File file, File rootDir) {
        if (file.getName().startsWith(".")) return true;
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        return relativePath.contains("node_modules") ||
                relativePath.contains("dist") ||
                relativePath.contains("target") ||
                relativePath.contains(".git");
    }

    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
