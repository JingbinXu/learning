package com.bing.bingaicode.core.review;

import com.bing.bingaicode.constant.AppConstant;
import com.bing.bingaicode.core.builder.VueProjectBuilder;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 代码审查流构建器
 * 在代码生成完成后，自动触发 AI 审查并以流式方式返回审查结果
 */
@Slf4j
@Component
public class ReviewStreamBuilder {

    private static final int CHUNK_SIZE = 100;
    private static final long BUILD_WAIT_TIMEOUT_MINUTES = 5;

    @Value("${code.review.enabled:true}")
    private boolean reviewEnabled;

    @Resource
    private CodeReviewService codeReviewService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 构建审查流
     *
     * @param appId       应用 ID
     * @param userMessage 用户原始提示词
     * @param codeGenType 代码生成类型
     * @return 审查内容的 Flux 流，失败时返回空流
     */
    public Flux<String> buildReviewFlux(long appId, String userMessage, CodeGenTypeEnum codeGenType) {
        if (!reviewEnabled) {
            return Flux.empty();
        }
        String codeDir = AppConstant.CODE_OUTPUT_ROOT_DIR
                + File.separator + codeGenType.getValue() + "_" + appId;

        return Mono.fromCallable(() -> {
                    // VUE_PROJECT 需要等待构建完成
                    if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
                        try {
                            Boolean buildResult = vueProjectBuilder.getCurrentBuildFuture()
                                    .get(BUILD_WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                            log.info("Vue 项目构建完成，结果: {}", buildResult);
                        } catch (Exception e) {
                            log.warn("等待 Vue 构建超时或异常: {}", e.getMessage());
                        }
                    }
                    // 读取代码文件
                    String codeContent = CodeFileReader.readAndConcatenateCodeFiles(codeDir);
                    if (codeContent.isBlank()) {
                        return "\n\n---\n\n## AI Code Review\n\n> 未找到可审查的代码文件。\n\n";
                    }
                    // 生成审查报告
                    String review = codeReviewService.generateCodeReview(userMessage, codeContent);
                    return "\n\n---\n\n## AI Code Review\n\n" + review + "\n\n";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(reviewText -> splitIntoChunks(reviewText, CHUNK_SIZE))
                .onErrorResume(e -> {
                    log.error("代码审查流程异常: {}", e.getMessage(), e);
                    return Flux.just("\n\n---\n\n## AI Code Review\n\n> 代码审查生成失败，请忽略此消息。\n\n");
                });
    }

    /**
     * 将文本按指定大小分割为 chunks，实现伪流式效果
     */
    private Flux<String> splitIntoChunks(String text, int chunkSize) {
        if (text.length() <= chunkSize) {
            return Flux.just(text);
        }
        return Flux.range(0, (text.length() + chunkSize - 1) / chunkSize)
                .map(i -> {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, text.length());
                    return text.substring(start, end);
                });
    }
}
