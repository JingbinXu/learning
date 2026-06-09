package com.bing.bingaicode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@MapperScan({"com.bing.bingaicode.mapper", "com.bing.bingaicode.rag.model"})
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class BingAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BingAiCodeApplication.class, args);
    }

}
