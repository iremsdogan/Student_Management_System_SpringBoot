package com.homework.project3.service;

import com.homework.project3.exception.ResourceNotFoundException;
import com.homework.project3.model.Enrollment;
import com.homework.project3.model.Student;
import com.homework.project3.model.Course;
import com.homework.project3.repository.EnrollmentRepository;
import com.homework.project3.repository.StudentRepository;
import com.homework.project3.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public Enrollment getEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID ile kayıt bulunamadı: " + id));
    }

    public Enrollment createEnrollment(Long courseId, Long studentId, Enrollment enrollmentDetails) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("ID ile kurs bulunamadı: " + courseId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("ID ile öğrenci bulunamadı: " + studentId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new IllegalArgumentException("Öğrenci zaten bu derse kayıtlı: " + studentId);
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setClassDate(enrollmentDetails.getClassDate());
        enrollment.setTuition(enrollmentDetails.getTuition());
        enrollment.setAttendance(enrollmentDetails.isAttendance());

        return enrollmentRepository.save(enrollment);
    }

    public Enrollment updateEnrollment(Long id, Enrollment updatedEnrollment) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID ile kayıt bulunamadı: " + id));

        if (updatedEnrollment.getStudent() == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        if (updatedEnrollment.getCourse() == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }

        Student student = studentRepository.findById(updatedEnrollment.getStudent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("ID ile öğrenci bulunamadı: " + updatedEnrollment.getStudent().getId()));
        Course course = courseRepository.findById(updatedEnrollment.getCourse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("ID ile kurs bulunamadı: " + updatedEnrollment.getCourse().getId()));

        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setClassDate(updatedEnrollment.getClassDate());
        enrollment.setTuition(updatedEnrollment.getTuition());
        enrollment.setAttendance(updatedEnrollment.isAttendance());

        return enrollmentRepository.save(enrollment);
    }

    public void deleteEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID ile kayıt bulunamadı: " + id));
        enrollmentRepository.delete(enrollment);
    }
}