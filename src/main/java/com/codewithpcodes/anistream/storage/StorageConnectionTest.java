package com.codewithpcodes.anistream.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConnectionTest {

    private final StorageService storageService;

    // Only runs in dev profile - remove after testing
    @Bean
    @Profile("dev")
    public CommandLineRunner testB2Connection() {
        return args -> {
            try {
                log.info("Testing Backblaze B2 connection...");
                ListBucketsResponse response = storageService.listBuckets();
                response.buckets().forEach(b -> log.info("Found bucket: {}", b.name()));
                log.info("Backblaze B2 connected successfully");
            } catch (Exception e) {
                log.error("Backblaze B2 connection failed: {}", e.getMessage());
            }
        };
    }
}
