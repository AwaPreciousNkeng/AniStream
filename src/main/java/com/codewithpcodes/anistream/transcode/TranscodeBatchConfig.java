package com.codewithpcodes.anistream.transcode;

import com.codewithpcodes.anistream.exception.TranscodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TranscodeBatchConfig {

    private final JobRepository jobRepository;
    private final TranscodingService transcodingService;
    private final TranscodeJobRepository  transcodeJobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job transcodeJob() {
        return new JobBuilder("transcodeJob", jobRepository)
                .start(transcodeStep())
                .build();
    }

    @Bean
    public Step transcodeStep() {
        return new StepBuilder("transcodeStep", jobRepository)
                .<TranscodeJob, TranscodeJob> chunk(1)
                .transactionManager(transactionManager)
                .reader(pendingJobReader())
                .processor(transcodeProcessor())
                .writer(jobWriter())
                .faultTolerant()
                .skip(TranscodingException.class)
                .skipLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<TranscodeJob> pendingJobReader() {
        return () -> transcodeJobRepository
                .findFirstByStatusOrderByCreatedAtDesc(
                        TranscodeStatus.PENDING
                )
                .orElse(null);
    }

    @Bean
    public ItemProcessor<TranscodeJob, TranscodeJob> transcodeProcessor() {
        return job -> {
            transcodingService.processJob(job);
            return job;
        };
    }

    @Bean
    public ItemWriter<TranscodeJob> jobWriter() {
        return jobs -> log.debug("Batch writer - {} jobs processed", jobs.getItems().size());
    }
}
