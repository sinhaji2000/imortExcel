package com.example.demo.service;

import com.example.demo.config.KafkaConfig;
import com.example.demo.enitity.ImportJob;
import com.example.demo.enitity.Student;
import com.example.demo.repository.ImportJobRepository;
import com.example.demo.repository.StudentRepository;

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
public class ImportJobConsumer {

    private final ImportJobRepository jobRepository;
    private final StudentRepository studentRepository;

    public ImportJobConsumer(
            ImportJobRepository jobRepository,
            StudentRepository studentRepository) {
        this.jobRepository = jobRepository;
        this.studentRepository = studentRepository;
    }

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

        // Avoid processing a job that has already finished.
        if ("COMPLETED".equals(job.getStatus())
                || "PROCESSING".equals(job.getStatus())) {
            return;
        }

        try {
            job.setStatus("PROCESSING");
            job.setErrorMessage(null);
            jobRepository.save(job);

            List<Student> students = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            Path filePath = Path.of(job.getFilePath());

            try (InputStream input = Files.newInputStream(filePath);
                 Workbook workbook = WorkbookFactory.create(input)) {

                Sheet sheet = workbook.getSheetAt(0);

                // Row 0 contains headers: name, email, age.
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);

                    if (row == null || isBlankRow(row, formatter)) {
                        continue;
                    }

                    String name = formatter
                            .formatCellValue(row.getCell(0)).trim();

                    String email = formatter
                            .formatCellValue(row.getCell(1)).trim();

                    // String ageText = formatter
                    //         .formatCellValue(row.getCell(2)).trim();

                    if (name.isEmpty() || email.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Required value missing at Excel row " + (i + 1));
                    }

                    // int age;
                    // try {
                    //     age = Integer.parseInt(ageText);
                    // } catch (NumberFormatException exception) {
                    //     throw new IllegalArgumentException(
                    //             "Invalid age at Excel row " + (i + 1));
                    // }

                    students.add(new Student(name, email));
                }
            }

            job.setTotalRows(students.size());
            jobRepository.save(job);

            // Save the parsed records after the full file has been read.
            studentRepository.saveAll(students);

            job.setProcessedRows(students.size());
            job.setFailedRows(0);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            jobRepository.save(job);

            System.out.println(
                    "Import completed. Job ID: " + jobId
                    + ", rows: " + students.size());

        } catch (Exception exception) {
            job.setStatus("FAILED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(shortMessage(exception));
            jobRepository.save(job);

            System.err.println(
                    "Import failed. Job ID: " + jobId
                    + ", reason: " + exception.getMessage());
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < 3; i++) {
            String value = formatter.formatCellValue(row.getCell(i)).trim();

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