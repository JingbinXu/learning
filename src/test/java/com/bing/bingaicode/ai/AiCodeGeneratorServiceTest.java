package com.bing.bingaicode.ai;

import com.bing.bingaicode.ai.model.HtmlCodeResult;
import com.bing.bingaicode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个关于cloudcode的博客，不超过20行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMutiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("做一个关于cloudcode的留言板，不超过50行");
        Assertions.assertNotNull(result);
    }
}