
package com.example.demo.service;

import com.example.demo.enitity.Student;
import com.example.demo.repository.StudentBatchRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentBatchService {

    private final StudentBatchRepository studentBatchRepository;

    @Transactional
    public void saveBatch(List<Student> batch) {
        studentBatchRepository.insertBatch(batch);
    }
}
