package com.example.demo.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data 
public class ExcelInputRequestDto {
    
    private MultipartFile file;
}
