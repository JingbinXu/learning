package com.bing.bingaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.bing.bingaicode.mapper")
@SpringBootApplication
public class BingAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BingAiCodeApplication.class, args);
    }

}
