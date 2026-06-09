-- ============================================================
-- OpenSpec 知识库相关表
-- ============================================================

-- 知识库文档表（OpenSpec 文档）
CREATE TABLE IF NOT EXISTS knowledge_base_doc
(
    id           BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    docName      VARCHAR(256)                       NOT NULL COMMENT '文档名称',
    docId        VARCHAR(128)                       NOT NULL COMMENT 'OpenSpec 文档唯一标识(spec_id)',
    category     VARCHAR(64)                        NOT NULL COMMENT '文档分类: design_system/component_library/coding_standard/layout_template/interaction_pattern/api_convention/project_template/security_rule/custom',
    description  VARCHAR(1024)                      NULL COMMENT '文档简介',
    tags         VARCHAR(1024)                      NULL COMMENT '标签列表(JSON数组)',
    frameworks   VARCHAR(512)                       NULL COMMENT '适用框架(JSON数组): vue3/react/html/multi_file',
    content      MEDIUMTEXT                         NOT NULL COMMENT '完整 OpenSpec YAML 内容',
    version      VARCHAR(32)  DEFAULT '1.0.0'       NOT NULL COMMENT '规范版本号',
    status       TINYINT      DEFAULT 1             NOT NULL COMMENT '状态: 0-禁用 1-启用',
    userId       BIGINT                             NOT NULL COMMENT '创建用户id',
    editTime     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT      DEFAULT 0             NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_docId (docId),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_userId (userId)
) COMMENT '知识库文档' COLLATE = utf8mb4_unicode_ci;

-- 知识库文档分块表（用于 RAG 检索）
CREATE TABLE IF NOT EXISTS knowledge_base_chunk
(
    id            BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    docId         VARCHAR(128)                       NOT NULL COMMENT '关联的文档ID(spec_id)',
    chunkIndex    INT                                NOT NULL COMMENT '分块在文档中的序号',
    chunkType     VARCHAR(64)                        NOT NULL COMMENT '分块类型: section/component/page_template/compliance_rule',
    sectionId     VARCHAR(128)                       NULL COMMENT '所属章节ID',
    title         VARCHAR(256)                       NULL COMMENT '分块标题',
    content       TEXT                               NOT NULL COMMENT '分块内容(纯文本+代码)',
    tags          VARCHAR(1024)                      NULL COMMENT '关联标签(JSON数组)',
    keywords      VARCHAR(1024)                      NULL COMMENT '关键词(用于全文检索)',
    embedding     JSON                               NULL COMMENT '向量嵌入(JSON数组, 预留字段)',
    status        TINYINT      DEFAULT 1             NOT NULL COMMENT '状态: 0-禁用 1-启用',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      DEFAULT 0             NOT NULL COMMENT '是否删除',
    INDEX idx_docId (docId),
    INDEX idx_chunkType (chunkType)
) COMMENT '知识库文档分块' COLLATE = utf8mb4_unicode_ci;
