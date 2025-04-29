package com.homework.project3.service;

import com.homework.project3.exception.ResourceNotFoundException;
import com.homework.project3.model.Student;
import com.homework.project3.model.Course;
import com.homework.project3.model.Enrollment;
import com.homework.project3.repository.StudentRepository;
import com.homework.project3.repository.CourseRepository;
import com.homework.project3.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FileStorageService fileStorageService;

    public StudentService(StudentRepository studentRepository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository, FileStorageService fileStorageService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Student> getAllStudents() {
        logger.info("Fetching all students");
        List<Student> students = studentRepository.findAll();
        logger.info("Retrieved {} students", students.size());
        return students;
    }

    public Student getStudentById(Long id) {
        logger.info("Fetching student with ID: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Student not found with ID: {}", id);
                    return new ResourceNotFoundException("Student not found with ID: " + id);
                });
        logger.info("Successfully retrieved student: {}", student);
        return student;
    }

    public Student getStudentByEmail(String email) {
        logger.info("Fetching student with email: {}", email);
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Student not found with email: {}", email);
                    return new ResourceNotFoundException("Student not found with email: " + email);
                });
        logger.info("Successfully retrieved student by email: {}", student);
        return student;
    }

    public Student addStudent(Student student) {
        logger.info("Attempting to add new student: {}", student);
        studentRepository.findByEmail(student.getEmail()).ifPresent(s -> {
            logger.error("Student with email {} already exists", student.getEmail());
            throw new IllegalArgumentException("There is already a student with this email: " + student.getEmail());
        });
        Student savedStudent = studentRepository.save(student);
        logger.info("Successfully added new student with ID: {}", savedStudent.getId());
        return savedStudent;
    }

    public Student updateStudent(Long id, Student newStudentData) {
        logger.info("Attempting to update student with ID: {}", id);
        logger.debug("New student data: {}", newStudentData);

        try {
            Student existingStudent = studentRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Student not found with ID: {}", id);
                        return new ResourceNotFoundException("Öğrenci bulunamadı: " + id);
                    });

            if (!existingStudent.getEmail().equals(newStudentData.getEmail())) {
                logger.info("Email change detected, checking for duplicates");
                studentRepository.findByEmail(newStudentData.getEmail())
                        .ifPresent(s -> {
                            logger.error("Email {} already exists", newStudentData.getEmail());
                            throw new IllegalArgumentException("Bu email adresi zaten kullanılıyor: " + newStudentData.getEmail());
                        });
            }

            if (newStudentData.getProfileImage() != null &&
                    existingStudent.getProfileImage() != null &&
                    !existingStudent.getProfileImage().equals(newStudentData.getProfileImage())) {
                try {
                    fileStorageService.deleteFile(existingStudent.getProfileImage());
                } catch (IOException e) {
                    logger.error("Failed to delete old profile image: {}", e.getMessage());
                }
            }

            logger.debug("Updating student fields");
            existingStudent.setName(newStudentData.getName());
            existingStudent.setSurname(newStudentData.getSurname());
            existingStudent.setEmail(newStudentData.getEmail());
            existingStudent.setDepartment(newStudentData.getDepartment());
            existingStudent.setProfileImage(newStudentData.getProfileImage());

            Student updatedStudent = studentRepository.save(existingStudent);
            logger.info("Successfully updated student with ID: {}", id);
            return updatedStudent;
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found while updating student: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument while updating student: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while updating student: {}", e.getMessage(), e);
            throw new RuntimeException("Öğrenci güncellenirken hata oluştu: " + e.getMessage(), e);
        }
    }

    public void deleteStudent(Long id) {
        logger.info("Attempting to delete student with ID: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Student not found with ID: {}", id);
                    return new ResourceNotFoundException("Student not found with ID: " + id);
                });


        if (student.getProfileImage() != null && !student.getProfileImage().isEmpty()) {
            try {
                fileStorageService.deleteFile(student.getProfileImage());
                logger.info("Successfully deleted profile image for student with ID: {}", id);
            } catch (IOException e) {
                logger.error("Failed to delete profile image for student with ID {}: {}", id, e.getMessage());
            }
        }

        studentRepository.delete(student);
    }

    public List<Course> getCoursesForStudent(Long studentId) {
        logger.info("Fetching courses for student with ID: {}", studentId);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<Course> courses = enrollments.stream().map(Enrollment::getCourse).collect(Collectors.toList());
        logger.info("Retrieved {} courses for student {}", courses.size(), studentId);
        return courses;
    }
}