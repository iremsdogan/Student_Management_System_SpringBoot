package com.homework.project3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.homework.project3.model.Course;

import java.util.Optional;

public interface CourseRepository  extends JpaRepository<Course, Long>{
    Optional<Course> findByName(String name);
}
