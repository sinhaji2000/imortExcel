package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enitity.ImportJob;


@Repository 
public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    
}
