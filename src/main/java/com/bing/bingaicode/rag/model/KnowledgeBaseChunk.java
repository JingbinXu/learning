package com.bing.bingaicode.rag.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档分块实体类（用于 RAG 检索）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_base_chunk")
public class KnowledgeBaseChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 关联的文档ID(spec_id) */
    @Column("docId")
    private String docId;

    /** 分块在文档中的序号 */
    @Column("chunkIndex")
    private Integer chunkIndex;

    /** 分块类型: section/component/page_template/compliance_rule */
    @Column("chunkType")
    private String chunkType;

    /** 所属章节ID */
    @Column("sectionId")
    private String sectionId;

    /** 分块标题 */
    private String title;

    /** 分块内容(纯文本+代码) */
    private String content;

    /** 关联标签(JSON数组) */
    private String tags;

    /** 关键词(用于全文检索) */
    private String keywords;

    /** 向量嵌入(JSON数组, 预留字段) */
    private String embedding;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
