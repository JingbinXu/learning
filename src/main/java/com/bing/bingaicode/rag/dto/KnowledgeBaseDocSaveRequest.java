package com.bing.bingaicode.rag.dto;

import lombok.Data;

/**
 * 知识库文档保存请求
 */
@Data
public class KnowledgeBaseDocSaveRequest {

    /** 文档ID（更新时传入） */
    private Long id;

    /** 文档名称 */
    private String docName;

    /** OpenSpec 文档唯一标识 */
    private String docId;

    /** 文档分类 */
    private String category;

    /** 文档简介 */
    private String description;

    /** 标签列表(逗号分隔) */
    private String tags;

    /** 适用框架(逗号分隔) */
    private String frameworks;

    /** OpenSpec YAML 内容 */
    private String content;

    /** 规范版本号 */
    private String version;
}
