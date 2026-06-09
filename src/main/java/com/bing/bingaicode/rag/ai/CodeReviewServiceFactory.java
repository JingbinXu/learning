package com.bing.bingaicode.rag.ai;

import com.bing.bingaicode.rag.model.CodeReviewResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码审查服务工厂
 */
@Slf4j
@Configuration
public class CodeReviewServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Bean
    public CodeReviewService createCodeReviewService() {
        return AiServices.builder(CodeReviewService.class)
                .chatModel(chatModel)
                .build();
    }
}
