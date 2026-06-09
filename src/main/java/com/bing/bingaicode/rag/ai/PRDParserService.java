package com.bing.bingaicode.rag.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * PRD 解析 AI 服务（Step 1: Planner Agent）
 * 将产品需求文档解析为扁平化的 JSON，手动解析以提高容错性
 */
public interface PRDParserService {

    /**
     * 解析 PRD 为 JSON 字符串（手动反序列化以容错）
     */
    @SystemMessage(fromResource = "prompt/prd-parser-system-prompt.txt")
    String parsePRD(@UserMessage String prdContent);
}
