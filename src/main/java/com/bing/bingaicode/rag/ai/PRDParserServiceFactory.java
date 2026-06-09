package com.bing.bingaicode.rag.ai;

import com.bing.bingaicode.rag.model.PRDParserResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PRD 解析服务工厂
 */
@Slf4j
@Configuration
public class PRDParserServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Bean
    public PRDParserService createPRDParserService() {
        return AiServices.builder(PRDParserService.class)
                .chatModel(chatModel)
                .build();
    }
}
