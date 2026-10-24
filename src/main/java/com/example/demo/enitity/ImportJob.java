package com.example.demo.enitity;

import lombok.Data;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@RequiredArgsConstructor 
@Data 
@Entity
@Table(name = "import_jobs")
public class ImportJob {
    
        @Id
        private String id;
    
        @Column(nullable = false, length = 1000)
        private String filePath;
    
        @Column(nullable = false)
        private String status;
    
        private LocalDateTime createdAt;

        public ImportJob(String id, String filePath, String status) {
                this.id = id;
                this.filePath = filePath;
                this.status = status;
                this.createdAt = LocalDateTime.now();
        }
}

