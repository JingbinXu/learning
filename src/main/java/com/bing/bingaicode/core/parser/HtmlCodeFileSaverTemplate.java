package com.bing.bingaicode.core.parser;

import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.ai.model.HtmlCodeResult;
import com.bing.bingaicode.core.saver.CodeFileSaverTemplate;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;

import static com.bing.bingaicode.core.CodeFileSaver.writeToFile;

/**
 * HTML代码文件保存器
 *
 * @author yupi
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {


    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML ;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath,"index.html",result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
    }


}
