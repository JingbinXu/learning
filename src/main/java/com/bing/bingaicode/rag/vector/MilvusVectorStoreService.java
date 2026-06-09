package com.bing.bingaicode.rag.vector;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Milvus 向量存储服务
 * 封装集合管理、向量写入和向量检索
 */
@Service
@Slf4j
public class MilvusVectorStoreService {

    private static final String FIELD_ID = "id";
    private static final String FIELD_DOC_ID = "docId";
    private static final String FIELD_CHUNK_TYPE = "chunkType";
    private static final String FIELD_EMBEDDING = "embedding";

    /** 向量维度 */
    private static final int VECTOR_DIMENSION = 1024;

    /** chunkType 白名单 —— 只允许已知的分块类型，防止 filter 表达式注入 */
    private static final Set<String> ALLOWED_CHUNK_TYPES = Set.of(
            "section", "component", "page_template", "compliance_rule"
    );

    /** 匹配安全标识符的正则：仅字母、数字、下划线 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Resource
    private MilvusServiceClient milvusClient;

    @Resource(name = "milvusCollectionName")
    private String collectionName;

    /**
     * 初始化集合（如果不存在）
     */
    public void initCollection() {
        if (milvusClient == null) return;
        try {
            // 检查集合是否存在
            R<Boolean> hasResp = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());
            if (hasResp.getData()) {
                log.info("Milvus 集合已存在: {}", collectionName);
                return;
            }
            // 创建集合 Schema
            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .withFieldTypes(Arrays.asList(
                            FieldType.newBuilder()
                                    .withName(FIELD_ID)
                                    .withDataType(DataType.Int64)
                                    .withPrimaryKey(true)
                                    .withAutoID(false)
                                    .build(),
                            FieldType.newBuilder()
                                    .withName(FIELD_DOC_ID)
                                    .withDataType(DataType.VarChar)
                                    .withMaxLength(128)
                                    .build(),
                            FieldType.newBuilder()
                                    .withName(FIELD_CHUNK_TYPE)
                                    .withDataType(DataType.VarChar)
                                    .withMaxLength(64)
                                    .build(),
                            FieldType.newBuilder()
                                    .withName(FIELD_EMBEDDING)
                                    .withDataType(DataType.FloatVector)
                                    .withDimension(VECTOR_DIMENSION)
                                    .build()
                    ))
                    .build();
            // 创建集合
            R<MutationResponse> createResp = milvusClient.createCollection(
                    CreateCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withSchema(schema)
                            .withShardsNum(2)
                            .build());
            if (createResp.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus 创建集合失败: {}", createResp.getMessage());
                return;
            }
            // 创建 IVF_FLAT 索引（适合中等规模数据）
            milvusClient.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName(FIELD_EMBEDDING)
                            .withIndexType(IndexType.IVF_FLAT)
                            .withMetricType(MetricType.COSINE)
                            .withExtraParam("{\"nlist\":128}")
                            .withSyncMode(true)
                            .build());
            // 加载集合到内存
            milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());
            log.info("Milvus 集合创建成功: {}", collectionName);
        } catch (Exception e) {
            log.error("Milvus 集合初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 插入向量（批量）
     *
     * @param chunkIds    分块 ID 列表（作为 Milvus 的主键）
     * @param docIds      文档 ID 列表
     * @param chunkTypes  分块类型列表
     * @param embeddings  向量列表
     * @return 是否成功
     */
    public boolean insertVectors(List<Long> chunkIds, List<String> docIds,
                                  List<String> chunkTypes, List<List<Float>> embeddings) {
        if (milvusClient == null) return false;
        if (chunkIds.isEmpty()) return true;
        try {
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field(FIELD_ID, chunkIds),
                    new InsertParam.Field(FIELD_DOC_ID, docIds),
                    new InsertParam.Field(FIELD_CHUNK_TYPE, chunkTypes),
                    new InsertParam.Field(FIELD_EMBEDDING, embeddings)
            );
            R<MutationResponse> resp = milvusClient.insert(
                    InsertParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFields(fields)
                            .build());
            boolean success = resp.getStatus() == R.Status.Success.getCode();
            if (success) {
                log.info("Milvus 向量插入成功，数量: {}", chunkIds.size());
            } else {
                log.error("Milvus 向量插入失败: {}", resp.getMessage());
            }
            return success;
        } catch (Exception e) {
            log.error("Milvus 向量插入异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 向量相似度检索
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回数量
     * @param chunkType      可选的分块类型过滤
     * @return 匹配的分块 ID 及其相似度分数
     */
    public Map<Long, Double> searchSimilar(List<Float> queryEmbedding, int topK, String chunkType) {
        if (milvusClient == null) return Collections.emptyMap();
        try {
            String filter = "";
            // 白名单校验 + 标识符安全检查，防止 Milvus filter 表达式注入
            if (chunkType != null && !chunkType.isEmpty()) {
                if (!ALLOWED_CHUNK_TYPES.contains(chunkType)) {
                    log.warn("非法的 chunkType，已忽略: {}", chunkType);
                } else if (!SAFE_IDENTIFIER.matcher(chunkType).matches()) {
                    log.warn("chunkType 包含非法字符，已忽略: {}", chunkType);
                } else {
                    filter = FIELD_CHUNK_TYPE + " == \"" + chunkType + "\"";
                }
            }
            SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectors(Collections.singletonList(queryEmbedding))
                    .withVectorFieldName(FIELD_EMBEDDING)
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                    .withOutFields(Arrays.asList(FIELD_ID, FIELD_DOC_ID, FIELD_CHUNK_TYPE));
            if (!filter.isEmpty()) {
                searchBuilder.withExpr(filter);
            }
            R<SearchResults> resp = milvusClient.search(searchBuilder.build());
            if (resp.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus 检索失败: {}", resp.getMessage());
                return Collections.emptyMap();
            }
            // 解析检索结果
            SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
            Map<Long, Double> results = new LinkedHashMap<>();
            if (!wrapper.getRowRecords(0).isEmpty()) {
                for (SearchResultsWrapper.IDScore idScore : wrapper.getIDScore(0)) {
                    long chunkId = Long.parseLong(idScore.get(FIELD_ID).toString());
                    double score = idScore.getScore();
                    results.put(chunkId, (double) score);
                }
            }
            log.debug("Milvus 检索完成，返回 {} 条结果", results.size());
            return results;
        } catch (Exception e) {
            log.error("Milvus 检索异常: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 删除指定文档的所有向量
     *
     * @param chunkIds 要删除的分块 ID 列表
     */
    public boolean deleteByChunkIds(List<Long> chunkIds) {
        if (milvusClient == null || chunkIds.isEmpty()) return false;
        try {
            String expr = FIELD_ID + " in " + chunkIds.toString();
            R<MutationResponse> resp = milvusClient.delete(
                    io.milvus.param.dml.DeleteParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withExpr(expr)
                            .build());
            return resp.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Milvus 向量删除异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查 Milvus 是否可用
     */
    public boolean isAvailable() {
        return milvusClient != null;
    }
}
