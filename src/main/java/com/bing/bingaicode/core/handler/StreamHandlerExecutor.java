package com.bing.bingaicode.core.handler;

import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. 传统的 Flux<String> 流（HTML、MULTI_FILE） -> SimpleTextStreamHandler
 * 2. TokenStream 格式的复杂流（VUE_PROJECT） -> JsonMessageStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 创建流处理器
     *
     * @param originFlux  原始流
     * @param appId       应用ID
     * @param codeGenType 代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  long appId, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT ->
                    jsonMessageStreamHandler.handle(originFlux, appId);
            case HTML, MULTI_FILE ->
                    new SimpleTextStreamHandler().handle(originFlux);
        };
    }
}
