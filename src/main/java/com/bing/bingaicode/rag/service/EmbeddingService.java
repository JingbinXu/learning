package com.bing.bingaicode.rag.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Embedding 向量服务
 * 使用阿里云 DashScope text-embedding-v3 模型生成文本向量
 * 内置 content hash 缓存，相同内容不重复调用 API
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final String DASHSCOPE_EMBEDDING_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private static final String MODEL_NAME = "text-embedding-v3";
    private static final int VECTOR_DIMENSION = 1024;
    private static final int BATCH_SIZE = 20;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    /**
     * embedding 缓存：content SHA256 → 向量
     * 写入后 1 小时过期，最大 5000 条（约覆盖 250 个文档的全部分块）
     */
    private final Cache<String, List<Float>> embeddingCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 生成单条文本的向量（带缓存）
     */
    public List<Float> embed(String text) {
        if (StrUtil.isBlank(text)) return Collections.emptyList();
        String hash = SecureUtil.sha256Hex(text);
        List<Float> cached = embeddingCache.getIfPresent(hash);
        if (cached != null) {
            log.debug("Embedding 缓存命中: hash={}", hash.substring(0, 8));
            return cached;
        }
        List<List<Float>> results = embedBatchInternal(List.of(text));
        if (!results.isEmpty() && !results.get(0).isEmpty()) {
            embeddingCache.put(hash, results.get(0));
            return results.get(0);
        }
        return Collections.emptyList();
    }

    /**
     * 批量生成文本向量（带缓存，只调 API 生成缓存未命中的）
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        List<List<Float>> allResults = new ArrayList<>(texts.size());
        List<Integer> missIndices = new ArrayList<>();
        List<String> missTexts = new ArrayList<>();

        // 第一轮：查缓存
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (StrUtil.isBlank(text)) {
                allResults.add(Collections.emptyList());
                continue;
            }
            String hash = SecureUtil.sha256Hex(text);
            List<Float> cached = embeddingCache.getIfPresent(hash);
            if (cached != null) {
                allResults.add(cached);
            } else {
                allResults.add(null); // 占位
                missIndices.add(i);
                missTexts.add(text);
            }
        }
        log.debug("Embedding 缓存统计: 总数={}, 命中={}, 未命中={}",
                texts.size(), texts.size() - missIndices.size(), missIndices.size());

        // 第二轮：批量调 API 生成未命中的
        if (!missTexts.isEmpty()) {
            List<List<Float>> apiResults = embedBatchInternal(missTexts);
            for (int j = 0; j < missIndices.size(); j++) {
                int originalIdx = missIndices.get(j);
                List<Float> vector = (j < apiResults.size()) ? apiResults.get(j) : Collections.emptyList();
                allResults.set(originalIdx, vector);
                // 写入缓存
                if (!vector.isEmpty()) {
                    String hash = SecureUtil.sha256Hex(missTexts.get(j));
                    embeddingCache.put(hash, vector);
                }
            }
        }
        return allResults;
    }

    /**
     * 内部方法：直接调 DashScope API 批量生成向量（无缓存）
     */
    private List<List<Float>> embedBatchInternal(List<String> texts) {
        if (StrUtil.isBlank(apiKey)) {
            log.warn("DashScope API Key 未配置，跳过向量生成");
            return Collections.emptyList();
        }
        List<List<Float>> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);
            try {
                allEmbeddings.addAll(callDashScopeEmbedding(batch));
            } catch (Exception e) {
                log.error("向量生成失败(批次 {}-{}): {}", i, end, e.getMessage());
                for (int j = 0; j < batch.size(); j++) {
                    allEmbeddings.add(Collections.emptyList());
                }
            }
        }
        return allEmbeddings;
    }

    /**
     * 调用 DashScope text-embedding-v3 API
     */
    private List<List<Float>> callDashScopeEmbedding(List<String> texts) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", MODEL_NAME);
        requestBody.set("input", new JSONObject().set("texts", texts));
        requestBody.set("parameters", new JSONObject().set("dimension", VECTOR_DIMENSION));

        HttpResponse response = HttpRequest.post(DASHSCOPE_EMBEDDING_URL)
                .header(Header.AUTHORIZATION.getValue(), "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE.getValue(), "application/json")
                .body(requestBody.toString())
                .timeout(30000)
                .execute();

        if (response.getStatus() != 200) {
            log.error("DashScope Embedding API 调用失败: status={}, body={}",
                    response.getStatus(), response.body());
            throw new RuntimeException("DashScope API 返回非 200: " + response.getStatus());
        }

        JSONObject responseBody = JSONUtil.parseObj(response.body());
        JSONArray embeddings = responseBody.getJSONObject("output").getJSONArray("embeddings");

        List<List<Float>> results = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            JSONObject item = embeddings.getJSONObject(i);
            JSONArray embeddingArray = item.getJSONArray("embedding");
            List<Float> vector = new ArrayList<>();
            for (int j = 0; j < embeddingArray.size(); j++) {
                vector.add(embeddingArray.getFloat(j));
            }
            results.add(vector);
        }
        log.debug("Embedding 生成成功，文本数: {}, 向量维度: {}",
                results.size(), results.isEmpty() ? 0 : results.get(0).size());
        return results;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vecA 向量 A
     * @param vecB 向量 B
     * @return 相似度 [-1, 1]，越接近 1 越相似
     */
    public static double cosineSimilarity(List<Float> vecA, List<Float> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty()) {
            return 0.0;
        }
        if (vecA.size() != vecB.size()) {
            log.warn("向量维度不匹配: {} vs {}", vecA.size(), vecB.size());
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
