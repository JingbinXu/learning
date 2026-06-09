package com.bing.bingaicode.rag.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.rag.model.KnowledgeBaseDoc;
import com.bing.bingaicode.rag.model.KnowledgeBaseDocMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * OpenSpec 默认规范初始化服务
 * 应用启动时自动扫描 specs/ 目录，导入默认规范文档
 */
@Service
@Slf4j
public class OpenSpecInitService {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeBaseDocMapper docMapper;

    /** 规范文件目录 */
    @Value("${openspec.import.dir:specs}")
    private String specDir;

    /** 默认导入的用户ID */
    private static final Long SYSTEM_USER_ID = 0L;

    /**
     * 应用启动后自动导入默认规范
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            importSpecsFromDirectory();
        } catch (Exception e) {
            log.warn("自动导入 OpenSpec 规范失败（不影响启动）: {}", e.getMessage());
        }
    }

    /**
     * 从 specs/ 目录导入所有 OpenSpec 文档
     */
    public int importSpecsFromDirectory() {
        File dir = new File(specDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.info("OpenSpec 规范目录不存在，跳过自动导入: {}", specDir);
            return 0;
        }
        File[] yamlFiles = dir.listFiles((d, name) ->
                name.endsWith(".yaml") || name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            log.info("OpenSpec 规范目录无 YAML 文件");
            return 0;
        }
        int imported = 0;
        for (File file : yamlFiles) {
            // 跳过 schema 文件
            if (file.getName().contains("schema")) {
                continue;
            }
            try {
                importSpecFile(file);
                imported++;
            } catch (Exception e) {
                log.warn("导入规范文件失败: {} - {}", file.getName(), e.getMessage());
            }
        }
        log.info("OpenSpec 规范自动导入完成，共导入 {} 个文档", imported);
        return imported;
    }

    /**
     * 导入单个规范文件
     */
    private void importSpecFile(File file) {
        String content = FileUtil.readUtf8String(file);
        if (StrUtil.isBlank(content)) {
            log.warn("规范文件为空: {}", file.getName());
            return;
        }
        // 简单解析 meta 信息（兼容 camelCase 和 snake_case）
        String specId = extractYamlValue(content, "specId");
        if (StrUtil.isBlank(specId)) specId = extractYamlValue(content, "spec_id");
        String specName = extractYamlValue(content, "specName");
        if (StrUtil.isBlank(specName)) specName = extractYamlValue(content, "spec_name");
        String description = extractYamlValue(content, "description");
        String version = extractYamlValue(content, "version");
        if (StrUtil.isBlank(specId)) {
            // 使用文件名作为 docId
            specId = file.getName().replace(".yaml", "").replace(".yml", "");
        }
        if (StrUtil.isBlank(specName)) {
            specName = specId;
        }
        // 检查是否已存在
        KnowledgeBaseDoc existingDoc = knowledgeBaseService.getDocByDocId(specId);
        if (existingDoc != null) {
            log.info("规范文档已存在，跳过导入: {}", specId);
            return;
        }
        // 创建文档
        KnowledgeBaseDoc doc = KnowledgeBaseDoc.builder()
                .docName(specName)
                .docId(specId)
                .category("custom")
                .description(description)
                .tags("[]")
                .frameworks("[]")
                .content(content)
                .version(StrUtil.isNotBlank(version) ? version : "1.0.0")
                .status(1)
                .userId(SYSTEM_USER_ID)
                .build();
        knowledgeBaseService.saveDoc(doc, SYSTEM_USER_ID);
        log.info("成功导入 OpenSpec 规范: {} ({})", specName, specId);
    }

    /**
     * 从 YAML 内容中简单提取值（仅支持单行字符串值）
     */
    private String extractYamlValue(String content, String key) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":")) {
                String value = trimmed.substring(key.length() + 1).trim();
                // 移除引号
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }
}
