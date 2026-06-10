package com.bing.bingaicode.core.review;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 代码审查服务
 * 使用独立的 AI 模型对生成的代码进行审查
 */
@Slf4j
@Service
public class CodeReviewService {

    @Resource(name = "codeReviewChatModel")
    private ChatModel chatModel;

    private CodeReviewAssistant assistant;

    private CodeReviewAssistant getAssistant() {
        if (assistant == null) {
            synchronized (this) {
                if (assistant == null) {
                    assistant = AiServices.builder(CodeReviewAssistant.class)
                            .chatModel(chatModel)
                            .build();
                }
            }
        }
        return assistant;
    }

    /**
     * 生成代码审查报告
     *
     * @param userMessage 用户原始提示词
     * @param codeContent 生成的代码内容
     * @return 审查报告（Markdown 格式）
     */
    public String generateCodeReview(String userMessage, String codeContent) {
        try {
            String prompt = String.format("用户的原始需求：%s\n\n以下是 AI 生成的代码：\n\n%s", userMessage, codeContent);
            return getAssistant().review(prompt);
        } catch (Exception e) {
            log.error("代码审查生成失败: {}", e.getMessage(), e);
            return "\n\n> 代码审查生成失败，请忽略此消息。\n";
        }
    }

    interface CodeReviewAssistant {
        @SystemMessage(fromResource = "prompt/code-review-system-prompt.txt")
        String review(@UserMessage String prompt);
    }
}
