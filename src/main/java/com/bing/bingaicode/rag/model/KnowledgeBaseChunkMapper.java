package com.bing.bingaicode.rag.model;

import com.mybatisflex.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档分块 Mapper
 */
@Mapper
public interface KnowledgeBaseChunkMapper extends BaseMapper<KnowledgeBaseChunk> {
}
