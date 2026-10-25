
package com.example.demo.service;

import com.example.demo.config.KafkaConfig;
import com.example.demo.enitity.ImportJob;
import com.example.demo.enitity.Student;
import com.example.demo.repository.ImportJobRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportJobConsumer {

    private static final int BATCH_SIZE = 100;

    private final ImportJobRepository jobRepository;
    private final StudentBatchService studentBatchService;

    @KafkaListener(
            topics = KafkaConfig.IMPORT_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:student-group}"
    )
    public void consume(String jobId) {

        System.out.println("Received Kafka job: " + jobId);

        ImportJob job = jobRepository.findById(jobId).orElse(null);

        if (job == null) {
            System.err.println("Import job not found: " + jobId);
            return;
        }

        if ("COMPLETED".equals(job.getStatus())
                || "PROCESSING".equals(job.getStatus())) {

            System.out.println(
                    "Skipping job with status: " + job.getStatus());
            return;
        }

        int totalRows = 0;
        int processedRows = 0;

        try {
            job.setStatus("PROCESSING");
            job.setErrorMessage(null);
            job.setCompletedAt(null);
            job.setTotalRows(0);
            job.setProcessedRows(0);
            job.setFailedRows(0);

            jobRepository.save(job);

            DataFormatter formatter = new DataFormatter();
            List<Student> batch = new ArrayList<>(BATCH_SIZE);

            Path filePath = Path.of(job.getFilePath());

            try (InputStream input = Files.newInputStream(filePath);
                 Workbook workbook = WorkbookFactory.create(input)) {

                if (workbook.getNumberOfSheets() == 0) {
                    throw new IllegalArgumentException(
                            "Excel file does not contain any sheet");
                }

                Sheet sheet = workbook.getSheetAt(0);

                // Row 0 contains the header: name, email, age.
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                    Row row = sheet.getRow(i);

                    if (row == null || isBlankRow(row, formatter)) {
                        continue;
                    }

                    String name = formatter
                            .formatCellValue(row.getCell(0))
                            .trim();

                    String email = formatter
                            .formatCellValue(row.getCell(1))
                            .trim();

                    if (name.isEmpty() || email.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Required name/email missing at Excel row "
                                        + (i + 1));
                    }

                    batch.add(new Student(name, email));
                    totalRows++;

                    if (batch.size() >= BATCH_SIZE) {

                        studentBatchService.saveBatch(batch);

                        processedRows += batch.size();
                        batch.clear();

                        updateProgress(job, totalRows, processedRows);

                        System.out.println(
                                "Progress: " + processedRows
                                        + " rows saved for job " + jobId);
                    }
                }

                // Save the last batch if fewer than BATCH_SIZE rows remain.
                if (!batch.isEmpty()) {

                    studentBatchService.saveBatch(batch);

                    processedRows += batch.size();
                    batch.clear();

                    updateProgress(job, totalRows, processedRows);
                }
            }

            job.setTotalRows(totalRows);
            job.setProcessedRows(processedRows);
            job.setFailedRows(0);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);

            jobRepository.save(job);

            System.out.println(
                    "Import completed. Job ID: " + jobId
                            + ", total rows: " + totalRows
                            + ", processed rows: " + processedRows);

        } catch (Exception exception) {

            job.setTotalRows(totalRows);
            job.setProcessedRows(processedRows);
            job.setFailedRows(Math.max(0, totalRows - processedRows));
            job.setStatus("FAILED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(shortMessage(exception));

            jobRepository.save(job);

            System.err.println(
                    "Import failed. Job ID: " + jobId
                            + ", reason: " + exception.getMessage());
        }
    }

    private void updateProgress(
            ImportJob job,
            int totalRows,
            int processedRows) {

        job.setTotalRows(totalRows);
        job.setProcessedRows(processedRows);

        jobRepository.save(job);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {

        for (int i = 0; i < 3; i++) {

            String value = formatter
                    .formatCellValue(row.getCell(i))
                    .trim();

            if (!value.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private String shortMessage(Exception exception) {

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        return message.length() > 1900
                ? message.substring(0, 1900)
                : message;
    }
}
