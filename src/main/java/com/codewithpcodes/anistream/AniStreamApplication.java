package com.codewithpcodes.anistream;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableRetry
@EnableScheduling
@EnableBatchProcessing
public class AniStreamApplication {

    static void main(String[] args) {
        SpringApplication.run(AniStreamApplication.class, args);
    }

}
