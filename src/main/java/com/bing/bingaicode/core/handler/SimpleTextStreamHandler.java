package com.bing.bingaicode.core.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     *
     * @param originFlux 原始流
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux) {
        return originFlux;
    }
}
