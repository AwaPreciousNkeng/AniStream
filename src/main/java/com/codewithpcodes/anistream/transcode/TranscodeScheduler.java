package com.codewithpcodes.anistream.transcode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodeScheduler {

    private final JobOperator jobOperator;
    private final Job transcodeJob;
    private final TranscodeJobRepository transcodeJobRepository;

    @Scheduled(fixedDelay = 30_000)
    public void triggerTranscodeJob() {

        //Only run if there are pending jobs
        boolean hasPending = transcodeJobRepository.findFirstByStatusOrderByCreatedAtAsc(
                TranscodeStatus.PENDING
        ).isPresent();

        if (!hasPending) {
            log.debug("No pending transcode jobs");
            return;
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobOperator.start(transcodeJob, params);

            log.info("Transcode batch started: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("Failed to launch transcode batch: {}", e.getMessage());
        }
    }
}
