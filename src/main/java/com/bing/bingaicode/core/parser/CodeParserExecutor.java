package com.bing.bingaicode.core.parser;

import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {
    private static final HtmlCodeParser htmlcodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();
//    执行代码解析
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum){
            case HTML -> htmlcodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> {throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");
            }
        };
    }
}
