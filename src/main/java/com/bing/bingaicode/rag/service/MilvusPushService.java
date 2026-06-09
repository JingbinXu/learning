package com.bing.bingaicode.rag.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import com.bing.bingaicode.rag.vector.MilvusVectorStoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 向量推送服务（独立 Bean，确保 @Async 通过代理调用生效）
 */
@Service
@Slf4j
public class MilvusPushService {

    @Resource
    private MilvusVectorStoreService milvusVectorStore;

    /**
     * 异步推送分块向量到 Milvus
     */
    @Async
    public void pushVectorsAsync(List<KnowledgeBaseChunk> chunks) {
        if (!milvusVectorStore.isAvailable() || chunks == null || chunks.isEmpty()) return;
        try {
            List<Long> chunkIds = new ArrayList<>();
            List<String> docIds = new ArrayList<>();
            List<String> chunkTypes = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();
            for (KnowledgeBaseChunk chunk : chunks) {
                List<Float> vec = parseEmbedding(chunk.getEmbedding());
                if (!vec.isEmpty()) {
                    chunkIds.add(chunk.getId());
                    docIds.add(chunk.getDocId());
                    chunkTypes.add(chunk.getChunkType());
                    vectors.add(vec);
                }
            }
            if (!chunkIds.isEmpty()) {
                boolean ok = milvusVectorStore.insertVectors(chunkIds, docIds, chunkTypes, vectors);
                log.info("Milvus 异步向量推送: count={}, result={}", chunkIds.size(), ok ? "成功" : "失败");
            }
        } catch (Exception e) {
            log.error("Milvus 异步向量推送失败: {}", e.getMessage(), e);
        }
    }

    private List<Float> parseEmbedding(String embeddingJson) {
        if (StrUtil.isBlank(embeddingJson)) return Collections.emptyList();
        try {
            return JSONUtil.toList(embeddingJson, Float.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
