package com.example.demo.controller;

import com.example.demo.dto.ExcelImportResponseDto;
import com.example.demo.dto.ExcelInputRequestDto;
import com.example.demo.service.ImportSubmissionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/students")
public class HomeController {
    

     private final ImportSubmissionService importService;

    
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExcelImportResponseDto importStudents(
            @ModelAttribute ExcelInputRequestDto request) {

        return importService.submit(request.getFile());
    }
}
