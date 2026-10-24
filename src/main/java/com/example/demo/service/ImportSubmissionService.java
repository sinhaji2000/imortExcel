package com.example.demo.service;



import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import com.example.demo.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ExcelImportResponseDto;
import com.example.demo.enitity.ImportJob;
import com.example.demo.exception.ExcelImportException;
import com.example.demo.repository.ImportJobRepository;


import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.* ;

@Service
@RequiredArgsConstructor 

public class ImportSubmissionService {

    private final ImportJobRepository jobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.import.storage-dir:./uploads/imports}")
    private  Path storageDirectory;
    

    

    public ExcelImportResponseDto submit(MultipartFile file) {
        validateFile(file);

        String jobId = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename();
        String extension = originalName.toLowerCase(Locale.ROOT)
                .endsWith(".xlsx") ? ".xlsx" : ".xls";

        Path storedFile = storageDirectory.resolve(jobId + extension);

        try {
            Files.createDirectories(storageDirectory);

            // Store the file before publishing its job ID.
            file.transferTo(storedFile);

            ImportJob job = new ImportJob(
                    jobId, storedFile.toString(), "PENDING");

            jobRepository.saveAndFlush(job);

            // Wait for Kafka acknowledgement so a failed publish
            // is not silently reported as a successful submission.
            kafkaTemplate.send(
                    KafkaConfig.IMPORT_TOPIC,
                    jobId,
                    jobId
            ).get(10, TimeUnit.SECONDS);

            return new ExcelImportResponseDto(
                    jobId,
                    "PENDING",
                    "Import job submitted successfully."
            );

        } catch (Exception exception) {
            // Do not delete the file or job record here: a timeout
            // can occur even if Kafka accepted the message.
            throw new ExcelImportException(
                    "Could not confirm import-job submission. "
                    + "Check the job record and Kafka before retrying.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelImportException(
                    "Upload a non-empty Excel file.");
        }

        String name = file.getOriginalFilename();

        if (name == null
                || !(name.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || name.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new ExcelImportException(
                    "Only .xlsx and .xls files are supported.");
        }

        // Retain the current 5 MB limit for the first integration.
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ExcelImportException(
                    "The Excel file must be no larger than 5 MB.");
        }
    }
}
