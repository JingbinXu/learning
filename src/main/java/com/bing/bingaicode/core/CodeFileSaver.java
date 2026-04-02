package com.bing.bingaicode.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.ai.model.HtmlCodeResult;
import com.bing.bingaicode.ai.model.MultiFileCodeResult;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {

    //文件保存的根目录
    private static final String FILE_SAVE_ROOT_DIR= System.getProperty("user.dir") + "/tmp/code_output" ;

    //保存HTML网页文件
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult){
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath,"index.html",htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);
    }
    //保存多文件代码
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }
    //构建文件的唯一路径：雪花id

    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    //保存单个文件


    public static void writeToFile(String dirPath,String fileName,String content){
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content,filePath, StandardCharsets.UTF_8);
    }
}
