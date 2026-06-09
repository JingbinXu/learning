package com.bing.bingaicode.rag.model;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档 Mapper
 */
@Mapper
public interface KnowledgeBaseDocMapper extends BaseMapper<KnowledgeBaseDoc> {
}
