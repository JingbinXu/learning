package com.bing.bingaicode.core;

import com.bing.bingaicode.ai.AiCodeGeneratorService;
import com.bing.bingaicode.ai.model.HtmlCodeResult;
import com.bing.bingaicode.ai.model.MultiFileCodeResult;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;


@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum){
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMutiFileCode(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码(流式)
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum){
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMutiFileCodeStream(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    private Flux<String> generateAndSaveMutiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMutiFileCodeStream(userMessage);
        //字符串拼接器，等流式返回完所有结果再保存
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk -> {
            codeBuilder.append(chunk);
        }).doOnComplete(() ->{
            //流式返回完成后，保存代码
            try {
                String completeMultiFileCode = codeBuilder.toString();
                //解析代码为对象
                MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
                //保存代码到文件
                File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                log.info("文件创建完成，目录为: {}",saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.info("保存失败{}",e.getMessage());
            }
        });
    }

    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        //字符串拼接器，等流式返回完所有结果再保存
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk -> {
            codeBuilder.append(chunk);
        }).doOnComplete(() ->{
            //流式返回完成后，保存代码
            try {
                String completeHtmlCode = codeBuilder.toString();
                //解析代码为对象
                HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
                //保存代码到文件
                File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                log.info("文件创建完成，目录为: {}",saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.info("保存失败{}",e.getMessage());
            }
        });
    }

    private File generateAndSaveMutiFileCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }

    private File generateAndSaveHtmlCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMutiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }

}
