package com.example.demo.controller;

import com.example.demo.dto.ExcelImportResponseDto;
import com.example.demo.dto.ExcelInputRequestDto;
import com.example.demo.dto.ImportJobStatusDto;
import com.example.demo.enitity.ImportJob;
import com.example.demo.repository.ImportJobRepository;
import com.example.demo.service.ImportSubmissionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class HomeController {

    private final ImportSubmissionService importService;
    private final ImportJobRepository jobRepository;

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExcelImportResponseDto importStudents(
            @ModelAttribute ExcelInputRequestDto request) {

        return importService.submit(request.getFile());
    }

    @GetMapping("/import/{jobId}")
    public ImportJobStatusDto getImportStatus(
            @PathVariable String jobId) {

        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Import job not found: " + jobId));

        return ImportJobStatusDto.from(job);
    }
}