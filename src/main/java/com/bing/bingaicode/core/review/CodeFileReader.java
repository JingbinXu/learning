package com.bing.bingaicode.core.review;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 代码文件读取工具类
 * 用于读取代码目录下的所有代码文件并拼接为文本内容
 */
public class CodeFileReader {

    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    private static final int MAX_TOTAL_CHARS = 50000;

    /**
     * 读取并拼接代码目录下的所有代码文件
     *
     * @param codeDir 代码目录路径
     * @return 拼接后的代码内容
     */
    public static String readAndConcatenateCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 项目文件结构和代码内容\n\n");
        FileUtil.walkFiles(directory, file -> {
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 文件: ").append(relativePath).append("\n\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });
        // 超出字符上限时截断
        if (codeContent.length() > MAX_TOTAL_CHARS) {
            codeContent.setLength(MAX_TOTAL_CHARS);
            codeContent.append("\n\n... [内容过长，已截断] ...");
        }
        return codeContent.toString();
    }

    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        if (file.getName().startsWith(".")) {
            return true;
        }
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
