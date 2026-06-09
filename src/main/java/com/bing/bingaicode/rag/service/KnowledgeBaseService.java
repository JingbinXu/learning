package com.bing.bingaicode.rag.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunkMapper;
import com.bing.bingaicode.rag.model.KnowledgeBaseDoc;
import com.bing.bingaicode.rag.model.KnowledgeBaseDocMapper;
import com.bing.bingaicode.rag.vector.MilvusVectorStoreService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务 - 负责 OpenSpec 文档的 CRUD 和 RAG 检索
 */
@Service
@Slf4j
public class KnowledgeBaseService {

    @Resource
    private KnowledgeBaseDocMapper docMapper;

    @Resource
    private KnowledgeBaseChunkMapper chunkMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private MilvusVectorStoreService milvusVectorStore;

    @Resource
    private MilvusPushService milvusPushService;

    @PostConstruct
    public void init() {
        // 应用启动时初始化 Milvus 集合
        milvusVectorStore.initCollection();
    }

    // ======================== CRUD 操作 ========================

    /**
     * 保存 OpenSpec 文档（新增或更新）
     *
     * @param doc    文档实体
     * @param userId 用户ID
     * @return 保存后的文档
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDoc saveDoc(KnowledgeBaseDoc doc, Long userId) {
        // 校验 YAML 格式
        validateYamlContent(doc.getContent());
        doc.setUserId(userId);
        doc.setEditTime(LocalDateTime.now());

        if (doc.getId() == null) {
            // 新增
            doc.setCreateTime(LocalDateTime.now());
            docMapper.insert(doc);
            log.info("新增知识库文档: {}", doc.getDocName());
        } else {
            // 更新
            docMapper.update(doc);
            log.info("更新知识库文档: {}", doc.getDocName());
            // 删除旧的分块
            deleteChunksByDocId(doc.getDocId());
        }
        // 解析并生成分块
        generateChunks(doc);
        return doc;
    }

    /**
     * 根据ID获取文档
     */
    public KnowledgeBaseDoc getDocById(Long id) {
        return docMapper.selectOneById(id);
    }

    /**
     * 根据 docId 获取文档
     */
    public KnowledgeBaseDoc getDocByDocId(String docId) {
        return docMapper.selectOne(
                QueryWrapper.create()
                        .eq("docId", docId)
                        .eq("isDelete", 0));
    }

    /**
     * 查询文档列表
     *
     * @param category 分类（可选）
     * @param keyword  关键词（可选）
     * @param status   状态（可选）
     * @return 文档列表
     */
    public List<KnowledgeBaseDoc> listDocs(String category, String keyword, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().eq("isDelete", 0);
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq("category", category);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like("docName", keyword)
                    .or().like("description", keyword)
                    .or().like("tags", keyword));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderBy("createTime desc");
        return docMapper.selectListByQuery(wrapper);
    }

    /**
     * 删除文档（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDoc(Long id, Long userId) {
        KnowledgeBaseDoc doc = docMapper.selectOneById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        docMapper.deleteById(id);
        deleteChunksByDocId(doc.getDocId());
        log.info("删除知识库文档: {}", doc.getDocName());
        return true;
    }

    /**
     * 启用/禁用文档
     */
    public boolean toggleDocStatus(Long id, Integer status) {
        KnowledgeBaseDoc doc = docMapper.selectOneById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        doc.setStatus(status);
        docMapper.update(doc);
        // 同步更新分块状态
        UpdateChain.of(KnowledgeBaseChunk.class)
                .set("status", status)
                .eq("docId", doc.getDocId())
                .update();
        return true;
    }

    // ======================== RAG 检索 ========================

    /**
     * 智能检索知识库：Milvus 向量检索（主）+ MySQL 关键词检索（辅）
     * 策略：优先 Milvus 向量相似度检索；Milvus 不可用时降级为 MySQL 余弦相似度
     */
    public List<KnowledgeBaseChunk> search(String query, String category, String framework, int topK) {
        if (StrUtil.isBlank(query)) {
            return Collections.emptyList();
        }
        log.info("RAG 检索 - 查询: {}, topK: {}", query, topK);

        List<Float> queryEmbedding = embeddingService.embed(query);
        boolean hasEmbedding = !queryEmbedding.isEmpty();
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        if (milvusVectorStore.isAvailable() && hasEmbedding) {
            // === 主路径：Milvus 向量检索 ===
            log.info("使用 Milvus 向量检索");
            Map<Long, Double> milvusResults = milvusVectorStore.searchSimilar(
                    queryEmbedding, topK * 3, null);
            if (!milvusResults.isEmpty()) {
                List<Long> chunkIds = new ArrayList<>(milvusResults.keySet());
                List<KnowledgeBaseChunk> chunks = chunkMapper.selectListByQuery(
                        QueryWrapper.create().in("id", chunkIds).eq("isDelete", 0).eq("status", 1));
                for (KnowledgeBaseChunk chunk : chunks) {
                    double vectorScore = milvusResults.getOrDefault(chunk.getId(), 0.0);
                    double keywordScore = computeKeywordScore(chunk, query);
                    double finalScore = vectorScore * 0.7 + keywordScore * 0.3;
                    if ("compliance_rule".equals(chunk.getChunkType())) finalScore += 0.1;
                    scoredChunks.add(new ScoredChunk(chunk, finalScore));
                }
            }
        } else if (hasEmbedding) {
            // === 降级路径：MySQL 全表 + Java 侧余弦相似度 ===
            log.info("Milvus 不可用，降级为 MySQL 余弦相似度检索");
            List<KnowledgeBaseChunk> allChunks = chunkMapper.selectListByQuery(
                    QueryWrapper.create().eq("isDelete", 0).eq("status", 1));
            for (KnowledgeBaseChunk chunk : allChunks) {
                List<Float> chunkEmbedding = parseEmbedding(chunk.getEmbedding());
                double vectorScore = chunkEmbedding.isEmpty() ? 0.0
                        : EmbeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
                double keywordScore = computeKeywordScore(chunk, query);
                double finalScore = vectorScore * 0.7 + keywordScore * 0.3;
                if ("compliance_rule".equals(chunk.getChunkType())) finalScore += 0.1;
                if (finalScore > 0.01) scoredChunks.add(new ScoredChunk(chunk, finalScore));
            }
        } else {
            // === 纯关键词降级 ===
            log.info("向量不可用，使用纯关键词检索");
            List<KnowledgeBaseChunk> allChunks = chunkMapper.selectListByQuery(
                    QueryWrapper.create().eq("isDelete", 0).eq("status", 1));
            for (KnowledgeBaseChunk chunk : allChunks) {
                double keywordScore = computeKeywordScore(chunk, query);
                if (keywordScore > 0.01) scoredChunks.add(new ScoredChunk(chunk, keywordScore));
            }
        }

        return scoredChunks.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(sc -> sc.chunk)
                .collect(Collectors.toList());
    }

    /**
     * 计算关键词匹配分数
     */
    private double computeKeywordScore(KnowledgeBaseChunk chunk, String query) {
        String lowerQuery = query.toLowerCase();
        String lowerTitle = chunk.getTitle() != null ? chunk.getTitle().toLowerCase() : "";
        String lowerContent = chunk.getContent() != null ? chunk.getContent().toLowerCase() : "";
        String lowerKeywords = chunk.getKeywords() != null ? chunk.getKeywords().toLowerCase() : "";
        double score = 0.0;
        if (lowerTitle.contains(lowerQuery)) score += 0.5;
        for (String word : lowerQuery.split("[\\s,\\uff0c;\\uff1b]+")) {
            if (word.length() < 2) continue;
            if (lowerKeywords.contains(word)) score += 0.2;
            if (lowerTitle.contains(word)) score += 0.15;
            if (lowerContent.contains(word)) score += 0.05;
        }
        return Math.min(score, 1.0);
    }

    private List<Float> parseEmbedding(String embeddingJson) {
        if (StrUtil.isBlank(embeddingJson)) return Collections.emptyList();
        try {
            return JSONUtil.toList(embeddingJson, Float.class);
        } catch (Exception e) {
            log.warn("解析 embedding 向量失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class ScoredChunk {
        final KnowledgeBaseChunk chunk;
        final double score;
        ScoredChunk(KnowledgeBaseChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    /**
     * 根据组件名称精确检索
     *
     * @param componentName 组件名称
     * @return 匹配的分块
     */
    public List<KnowledgeBaseChunk> searchByComponent(String componentName) {
        if (StrUtil.isBlank(componentName)) {
            return Collections.emptyList();
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .eq("status", 1)
                .eq("chunkType", "component")
                .and(w -> w
                        .like("title", componentName)
                        .or().like("content", componentName)
                        .or().like("keywords", componentName));
        return chunkMapper.selectListByQuery(wrapper);
    }

    /**
     * 根据分类获取所有启用的规范分块
     *
     * @param chunkType 分块类型
     * @return 分块列表
     */
    public List<KnowledgeBaseChunk> getChunksByType(String chunkType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .eq("status", 1)
                .eq("chunkType", chunkType)
                .orderBy("chunkIndex asc");
        return chunkMapper.selectListByQuery(wrapper);
    }

    /**
     * 将检索到的分块组装为可注入 Prompt 的规范文本
     *
     * @param chunks 检索到的分块列表
     * @return 格式化的规范文本
     */
    public String assembleChunksToContext(List<KnowledgeBaseChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## 企业前端规范（以下规范必须严格遵守）\n\n");

        Map<String, List<KnowledgeBaseChunk>> grouped = chunks.stream()
                .collect(Collectors.groupingBy(KnowledgeBaseChunk::getChunkType));

        // 合规规则优先
        if (grouped.containsKey("compliance_rule")) {
            sb.append("### 必须遵守的合规规则（违反将导致代码审查不通过）\n\n");
            for (KnowledgeBaseChunk chunk : grouped.get("compliance_rule")) {
                sb.append(chunk.getContent()).append("\n\n");
            }
        }

        // 设计系统
        if (grouped.containsKey("section")) {
            sb.append("### 设计规范\n\n");
            for (KnowledgeBaseChunk chunk : grouped.get("section")) {
                sb.append("#### ").append(chunk.getTitle()).append("\n\n");
                sb.append(chunk.getContent()).append("\n\n");
            }
        }

        // 组件库
        if (grouped.containsKey("component")) {
            sb.append("### 必须使用的企业组件（禁止使用原生 HTML 元素替代）\n\n");
            for (KnowledgeBaseChunk chunk : grouped.get("component")) {
                sb.append(chunk.getContent()).append("\n\n");
            }
        }

        // 页面模板
        if (grouped.containsKey("page_template")) {
            sb.append("### 参考页面模板\n\n");
            for (KnowledgeBaseChunk chunk : grouped.get("page_template")) {
                sb.append(chunk.getContent()).append("\n\n");
            }
        }

        return sb.toString();
    }

    // ======================== 内部方法 ========================

    /**
     * 校验 YAML 内容
     */
    @SuppressWarnings("unchecked")
    private void validateYamlContent(String content) {
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档内容不能为空");
        }
        Object parsed;
        try {
            Yaml yaml = new Yaml();
            parsed = yaml.load(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的 YAML 格式: " + e.getMessage());
        }
        if (!(parsed instanceof Map)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "YAML 内容必须是一个对象（map 结构）");
        }
        Map<String, Object> root = (Map<String, Object>) parsed;
        // 校验 meta 字段
        Object metaObj = root.get("meta");
        if (metaObj instanceof Map) {
            Map<String, Object> meta = (Map<String, Object>) metaObj;
            // 兼容 camelCase 和 snake_case
            String specId = getOrDefault(meta, "specId", "spec_id", "");
            String specName = getOrDefault(meta, "specName", "spec_name", "");
            if (StrUtil.isBlank(specId)) {
                log.warn("OpenSpec 文档缺少 meta.specId 字段，将使用文件名作为标识");
            }
            if (StrUtil.isBlank(specName)) {
                log.warn("OpenSpec 文档缺少 meta.specName 字段");
            }
        } else {
            log.warn("OpenSpec 文档缺少 meta 字段，跳过 meta 校验");
        }
        // 校验至少有一个内容板块
        boolean hasSections = root.containsKey("sections") || root.containsKey("components")
                || root.containsKey("pageTemplates") || root.containsKey("page_templates")
                || root.containsKey("complianceRules") || root.containsKey("compliance_rules");
        if (!hasSections) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "OpenSpec 文档必须至少包含一个内容板块: sections / components / pageTemplates / complianceRules");
        }
        log.info("YAML 校验通过");
    }

    /**
     * 兼容 camelCase 和 snake_case 两种 key
     */
    private String getOrDefault(Map<String, Object> map, String camelKey, String snakeKey, String defaultVal) {
        Object val = map.get(camelKey);
        if (val == null) val = map.get(snakeKey);
        return val != null ? val.toString() : defaultVal;
    }

    /**
     * 解析 OpenSpec 文档并生成分块
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateChunks(KnowledgeBaseDoc doc) {
        try {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) yaml.load(doc.getContent());
            List<KnowledgeBaseChunk> chunks = new ArrayList<>();
            int index = 0;

            // 解析 meta（兼容 camelCase 和 snake_case）
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = getMapFromSpec(spec, "meta");
            List<String> tags = parseStringListFromMap(meta, "tags");
            List<String> frameworks = parseStringListFromMap(meta, "applicableFrameworks", "applicable_frameworks");

            // 解析 sections
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections = getListFromSpec(spec, "sections", "sections");
            for (Map<String, Object> section : sections) {
                String sectionId = (String) section.getOrDefault("sectionId", "section_" + index);
                String title = (String) section.getOrDefault("title", "");
                StringBuilder content = new StringBuilder();
                content.append("# ").append(title).append("\n\n");
                Object contentObj = section.get("content");
                if (contentObj != null) {
                    content.append(contentObj.toString()).append("\n\n");
                }
                // 添加代码示例
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> codeExamples = (List<Map<String, Object>>) section.getOrDefault("codeExamples", Collections.emptyList());
                for (Map<String, Object> example : codeExamples) {
                    String lang = (String) example.getOrDefault("language", "");
                    String desc = (String) example.getOrDefault("description", "");
                    String code = (String) example.getOrDefault("code", "");
                    if (StrUtil.isNotBlank(desc)) {
                        content.append("示例：").append(desc).append("\n");
                    }
                    content.append("```").append(lang).append("\n").append(code).append("\n```\n\n");
                }
                // 添加规则
                @SuppressWarnings("unchecked")
                List<String> rules = (List<String>) section.getOrDefault("rules", Collections.emptyList());
                if (!rules.isEmpty()) {
                    content.append("规则：\n");
                    for (String rule : rules) {
                        content.append("- ").append(rule).append("\n");
                    }
                }
                chunks.add(KnowledgeBaseChunk.builder()
                        .docId(doc.getDocId())
                        .chunkIndex(index++)
                        .chunkType("section")
                        .sectionId(sectionId)
                        .title(title)
                        .content(content.toString())
                        .tags(JSONUtil.toJsonStr(tags))
                        .keywords(extractKeywords(title + " " + content))
                        .status(1)
                        .build());
            }

            // 解析 components
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> components = getListFromSpec(spec, "components", "components");
            for (Map<String, Object> component : components) {
                String name = (String) component.getOrDefault("componentName", "");
                @SuppressWarnings("unchecked")
                List<String> aliases = (List<String>) component.getOrDefault("componentAlias", Collections.emptyList());
                StringBuilder content = new StringBuilder();
                content.append("# 组件: ").append(name).append("\n\n");
                Object desc = component.get("description");
                if (desc != null) content.append("描述: ").append(desc).append("\n\n");
                Object usage = component.get("usage");
                if (usage != null) content.append("使用场景: ").append(usage).append("\n\n");

                // props
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> props = (List<Map<String, Object>>) component.getOrDefault("props", Collections.emptyList());
                if (!props.isEmpty()) {
                    content.append("属性:\n");
                    for (Map<String, Object> prop : props) {
                        content.append("- ").append(prop.get("name"))
                                .append(" (").append(prop.getOrDefault("type", "")).append(")")
                                .append(prop.getOrDefault("required", false).equals(true) ? " [必填]" : "")
                                .append(": ").append(prop.getOrDefault("description", "")).append("\n");
                    }
                    content.append("\n");
                }

                // 代码示例
                Object codeExample = component.get("codeExample");
                if (codeExample != null) {
                    content.append("使用示例:\n```html\n").append(codeExample).append("\n```\n\n");
                }

                // 替代组件
                Object replaces = component.get("replaces");
                if (replaces != null) {
                    content.append("替代以下原生/第三方组件: ").append(replaces).append("\n\n");
                }

                // 反模式
                @SuppressWarnings("unchecked")
                List<String> antiPatterns = (List<String>) component.getOrDefault("antiPatterns", Collections.emptyList());
                if (!antiPatterns.isEmpty()) {
                    content.append("禁止用法:\n");
                    for (String ap : antiPatterns) {
                        content.append("- ").append(ap).append("\n");
                    }
                }

                String keywords = String.join(" ", name, String.join(" ", aliases));
                chunks.add(KnowledgeBaseChunk.builder()
                        .docId(doc.getDocId())
                        .chunkIndex(index++)
                        .chunkType("component")
                        .sectionId("comp_" + name.toLowerCase())
                        .title(name)
                        .content(content.toString())
                        .tags(JSONUtil.toJsonStr(tags))
                        .keywords(keywords)
                        .status(1)
                        .build());
            }

            // 解析 pageTemplates
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pageTemplates = getListFromSpec(spec, "pageTemplates", "page_templates");
            for (Map<String, Object> template : pageTemplates) {
                String name = (String) template.getOrDefault("templateName", "");
                StringBuilder content = new StringBuilder();
                content.append("# 页面模板: ").append(name).append("\n\n");
                Object desc = template.get("description");
                if (desc != null) content.append("描述: ").append(desc).append("\n\n");
                @SuppressWarnings("unchecked")
                List<String> scenarios = (List<String>) template.getOrDefault("applicableScenarios", Collections.emptyList());
                if (!scenarios.isEmpty()) {
                    content.append("适用场景: ").append(String.join(", ", scenarios)).append("\n\n");
                }
                Object structure = template.get("structure");
                if (structure != null) content.append("页面结构:\n").append(structure).append("\n\n");
                Object codeExample = template.get("codeExample");
                if (codeExample != null) {
                    content.append("参考代码:\n```html\n").append(codeExample).append("\n```\n\n");
                }
                chunks.add(KnowledgeBaseChunk.builder()
                        .docId(doc.getDocId())
                        .chunkIndex(index++)
                        .chunkType("page_template")
                        .sectionId("tmpl_" + name.toLowerCase().replace(" ", "_"))
                        .title(name)
                        .content(content.toString())
                        .tags(JSONUtil.toJsonStr(tags))
                        .keywords(extractKeywords(name + " " + desc))
                        .status(1)
                        .build());
            }

            // 解析 complianceRules
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> complianceRules = getListFromSpec(spec, "complianceRules", "compliance_rules");
            for (Map<String, Object> rule : complianceRules) {
                String ruleId = (String) rule.getOrDefault("ruleId", "");
                StringBuilder content = new StringBuilder();
                content.append("【").append(rule.getOrDefault("severity", "warning")).append("】");
                content.append(" ").append(rule.getOrDefault("description", "")).append("\n");
                Object fixSuggestion = rule.get("fixSuggestion");
                if (fixSuggestion != null) {
                    content.append("修复建议: ").append(fixSuggestion).append("\n");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> examples = (Map<String, Object>) rule.getOrDefault("examples", Collections.emptyMap());
                if (examples.containsKey("correct")) {
                    content.append("正确: ").append(examples.get("correct")).append("\n");
                }
                if (examples.containsKey("incorrect")) {
                    content.append("错误: ").append(examples.get("incorrect")).append("\n");
                }
                chunks.add(KnowledgeBaseChunk.builder()
                        .docId(doc.getDocId())
                        .chunkIndex(index++)
                        .chunkType("compliance_rule")
                        .sectionId(ruleId)
                        .title(ruleId)
                        .content(content.toString())
                        .tags(JSONUtil.toJsonStr(tags))
                        .keywords(extractKeywords(ruleId + " " + rule.getOrDefault("description", "")))
                        .status(1)
                        .build());
            }

            // 为每个分块生成 embedding 向量
            log.info("开始为 {} 个分块生成 embedding 向量...", chunks.size());
            List<String> textsToEmbed = chunks.stream()
                    .map(chunk -> {
                        // 向量化内容 = 标题 + 内容摘要（截取前 500 字避免超长）
                        String text = (chunk.getTitle() != null ? chunk.getTitle() + "\n" : "")
                                + chunk.getContent();
                        return text.length() > 500 ? text.substring(0, 500) : text;
                    })
                    .collect(Collectors.toList());
            List<List<Float>> embeddings = embeddingService.embedBatch(textsToEmbed);
            // 将向量写入分块
            for (int i = 0; i < chunks.size(); i++) {
                if (i < embeddings.size() && !embeddings.get(i).isEmpty()) {
                    chunks.get(i).setEmbedding(JSONUtil.toJsonStr(embeddings.get(i)));
                }
            }
            long embeddedCount = chunks.stream()
                    .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
                    .count();
            log.info("Embedding 生成完成: {}/{} 个分块已向量化", embeddedCount, chunks.size());

            // 批量插入分块
            for (KnowledgeBaseChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }
            log.info("文档 {} 已拆分为 {} 个分块", doc.getDocName(), chunks.size());

            // 推送向量到 Milvus（通过独立 Bean 异步调用，确保 @Async 生效）
            milvusPushService.pushVectorsAsync(chunks);

        } catch (Exception e) {
            log.error("解析 OpenSpec 文档失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析 OpenSpec 文档失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档的所有分块（同步清理 Milvus 向量）
     */
    private void deleteChunksByDocId(String docId) {
        QueryWrapper wrapper = QueryWrapper.create().eq("docId", docId);
        List<KnowledgeBaseChunk> chunks = chunkMapper.selectListByQuery(wrapper);
        if (chunks.isEmpty()) return;

        // 先清理 Milvus 向量（在 MySQL 删除前拿到 chunk ID 列表）
        if (milvusVectorStore.isAvailable()) {
            List<Long> chunkIds = chunks.stream()
                    .map(KnowledgeBaseChunk::getId)
                    .collect(Collectors.toList());
            boolean deleted = milvusVectorStore.deleteByChunkIds(chunkIds);
            log.info("Milvus 向量清理: docId={}, chunkCount={}, result={}",
                    docId, chunkIds.size(), deleted ? "成功" : "失败");
        }

        // 再删除 MySQL 分块
        for (KnowledgeBaseChunk chunk : chunks) {
            chunkMapper.deleteById(chunk.getId());
        }
        log.info("MySQL 分块清理: docId={}, chunkCount={}", docId, chunks.size());
    }

    /**
     * 从查询文本中提取关键词
     */
    private String extractKeywords(String text) {
        if (StrUtil.isBlank(text)) return "";
        // 移除标点符号和特殊字符，提取有意义的词
        String cleaned = text.replaceAll("[\\p{Punct}\\p{IsPunctuation}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // 去除常见停用词
        Set<String> stopWords = Set.of("的", "了", "在", "是", "我", "有", "和", "就",
                "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
                "你", "会", "着", "没有", "看", "好", "自己", "这", "他", "她", "它",
                "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did", "will", "would", "could",
                "should", "may", "might", "shall", "can", "need", "must",
                "and", "or", "but", "if", "then", "else", "for", "to", "of", "in",
                "on", "at", "by", "with", "from", "as", "into", "through", "during",
                "before", "after", "above", "below", "between", "out", "off", "over",
                "under", "again", "further", "once", "here", "there", "when", "where",
                "why", "how", "all", "each", "every", "both", "few", "more", "most",
                "other", "some", "such", "no", "nor", "not", "only", "own", "same",
                "so", "than", "too", "very", "just", "because", "about", "that", "this",
                "which", "who", "whom", "what", "these", "those", "i", "me", "my", "we",
                "our", "you", "your", "he", "him", "his", "she", "her", "its", "they",
                "them", "their", "it");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> w.length() >= 2 && !stopWords.contains(w.toLowerCase()))
                .distinct()
                .collect(Collectors.joining(" "));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromSpec(Map<String, Object> spec, String key) {
        Object val = spec.get(key);
        return val instanceof Map ? (Map<String, Object>) val : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListFromSpec(Map<String, Object> spec,
                                                       String camelKey, String snakeKey) {
        Object val = spec.get(camelKey);
        if (val == null) val = spec.get(snakeKey);
        if (val instanceof List) return (List<Map<String, Object>>) val;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringListFromMap(Map<String, Object> map, String camelKey, String snakeKey) {
        Object val = map.get(camelKey);
        if (val == null) val = map.get(snakeKey);
        if (val instanceof List) return (List<String>) val;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringListFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<String>) val;
        return Collections.emptyList();
    }

    /**
     * 从 Map 中获取字符串值（兼容 camelCase 和 snake_case）
     */
    private String getStr(Map<String, Object> map, String camelKey, String snakeKey) {
        Object val = map.get(camelKey);
        if (val == null) val = map.get(snakeKey);
        return val != null ? val.toString() : "";
    }

    /**
     * 从 Map 中获取字符串值（单 key）
     */
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
