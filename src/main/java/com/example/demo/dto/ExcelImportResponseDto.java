package com.example.demo.dto;

public record ExcelImportResponseDto(
        String jobId,
        String status,
        String message
) {
}
