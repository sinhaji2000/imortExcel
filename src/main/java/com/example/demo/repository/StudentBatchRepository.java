
package com.example.demo.repository;

import com.example.demo.enitity.Student;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudentBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insertBatch(List<Student> students) {

        String sql = """
                INSERT INTO students (name, email)
                VALUES (?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                students,
                100,
                (ps, student) -> {
                    ps.setString(1, student.getName());
                    ps.setString(2, student.getEmail());
                }
        );
    }
}
