package com.bing.bingaicode.rag.dto;

import com.bing.bingaicode.rag.model.KnowledgeBaseChunk;
import lombok.Data;

import java.util.List;

/**
 * RAG 检索响应
 */
@Data
public class RAGSearchResponse {

    /** 原始查询 */
    private String query;

    /** 检索到的规范上下文文本（可直接注入 Prompt） */
    private String assembledContext;

    /** 检索到的原始分块列表 */
    private List<KnowledgeBaseChunk> chunks;

    /** 检索到的分块数量 */
    private int chunkCount;

    public static RAGSearchResponse of(String query, List<KnowledgeBaseChunk> chunks, String assembledContext) {
        RAGSearchResponse response = new RAGSearchResponse();
        response.setQuery(query);
        response.setChunks(chunks);
        response.setAssembledContext(assembledContext);
        response.setChunkCount(chunks.size());
        return response;
    }
}
