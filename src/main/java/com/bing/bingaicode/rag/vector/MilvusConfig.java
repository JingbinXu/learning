package com.bing.bingaicode.rag.vector;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库连接配置
 */
@Configuration
@Slf4j
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.collection:openspec_knowledge}")
    private String collectionName;

    @Value("${milvus.enabled:false}")
    private boolean enabled;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        if (!enabled) {
            log.info("Milvus 未启用，使用 MySQL 降级方案");
            return null;
        }
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            log.info("Milvus 连接成功: {}:{}", host, port);
            return client;
        } catch (Exception e) {
            log.warn("Milvus 连接失败，降级为 MySQL: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public String milvusCollectionName() {
        return collectionName;
    }

    @PreDestroy
    public void close() {
        // client 关闭由 Spring 管理
    }
}
