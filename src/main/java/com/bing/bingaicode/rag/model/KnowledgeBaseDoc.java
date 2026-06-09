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
 * 知识库文档实体类（OpenSpec 文档）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_base_doc")
public class KnowledgeBaseDoc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 文档名称 */
    @Column("docName")
    private String docName;

    /** OpenSpec 文档唯一标识 */
    @Column("docId")
    private String docId;

    /** 文档分类 */
    @Column("category")
    private String category;

    /** 文档简介 */
    private String description;

    /** 标签列表(JSON数组) */
    private String tags;

    /** 适用框架(JSON数组) */
    private String frameworks;

    /** 完整 OpenSpec YAML 内容 */
    private String content;

    /** 规范版本号 */
    private String version;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 创建用户id */
    @Column("userId")
    private Long userId;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
