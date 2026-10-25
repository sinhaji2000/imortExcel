package com.example.demo.dto;

import com.example.demo.enitity.ImportJob;
import java.time.LocalDateTime;

public record ImportJobStatusDto(
        String jobId,
        String status,
        int totalRows,
        int processedRows,
        int failedRows,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static ImportJobStatusDto from(ImportJob job) {
        return new ImportJobStatusDto(
                job.getId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getFailedRows(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}