package com.codewithpcodes.anistream.admin;

public record AdminStatsResponse(
        long totalMedia,
        long pendingTranscodeJobs,
        long runningTranscodeJobs,
        long failedTranscodeJobs
) {}
