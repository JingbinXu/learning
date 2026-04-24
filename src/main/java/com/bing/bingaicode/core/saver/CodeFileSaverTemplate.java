package com.bing.bingaicode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.constant.AppConstant;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;

import java.io.File;

public abstract class CodeFileSaverTemplate<T> {

    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;
    public final File saveCode (T result,Long  appId){
        //验证输入
        validateInput(result);
        //构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        //保存文件（具体实现交给子类）
        saveFiles(result, baseDirPath);
        //返回文件目录对象
        return new File(baseDirPath);
    }


    protected void validateInput(T result){
        if(result == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
    }
    protected String buildUniqueDir(Long appId){
        if(appId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }


    protected abstract void saveFiles(T result, String baseDirPath);

    protected abstract CodeGenTypeEnum getCodeType();
}
