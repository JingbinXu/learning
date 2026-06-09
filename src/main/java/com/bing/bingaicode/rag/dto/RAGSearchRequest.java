package com.bing.bingaicode.rag.dto;

import lombok.Data;

/**
 * RAG 检索请求
 */
@Data
public class RAGSearchRequest {

    /** 检索查询文本 */
    private String query;

    /** 分类过滤（可选） */
    private String category;

    /** 框架过滤（可选） */
    private String framework;

    /** 返回结果数量 */
    private Integer topK;
}
