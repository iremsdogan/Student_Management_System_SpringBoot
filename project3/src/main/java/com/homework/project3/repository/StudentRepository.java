package com.homework.project3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.homework.project3.model.Student;

import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
    public interface StudentRepository extends JpaRepository<Student, Long> {
        Optional<Student> findByEmail(String email);
}

