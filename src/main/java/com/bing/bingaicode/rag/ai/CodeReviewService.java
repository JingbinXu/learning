package com.bing.bingaicode.rag.ai;

import com.bing.bingaicode.rag.model.CodeReviewResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码审查 AI 服务（Step 4: Reviewer Agent）
 * 检查生成的代码是否符合企业规范
 */
public interface CodeReviewService {

    /**
     * 审查代码是否符合企业规范
     *
     * @param codeWithSpec 代码内容 + 企业规范上下文
     * @return 审查结果
     */
    @SystemMessage(fromResource = "prompt/code-review-system-prompt.txt")
    CodeReviewResult reviewCode(@UserMessage String codeWithSpec);
}
