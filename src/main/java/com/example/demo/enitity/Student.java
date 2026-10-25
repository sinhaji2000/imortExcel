package com.example.demo.enitity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.*;

@RequiredArgsConstructor 
@Data 
@Entity
@Table(name = "students")
public class Student {
    
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    //private Integer age;
}
