package com.homework.project3.service;

import com.homework.project3.exception.ResourceNotFoundException;
import com.homework.project3.model.Course;
import com.homework.project3.model.Student;
import com.homework.project3.model.Enrollment;
import com.homework.project3.repository.CourseRepository;
import com.homework.project3.repository.StudentRepository;
import com.homework.project3.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(CourseRepository courseRepository, StudentRepository studentRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    public Course addCourse(Course course) {
        courseRepository.findByName(course.getName()).ifPresent(c -> {
            throw new IllegalArgumentException("There is already a course with this name: " + course.getName());
        });
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course newCourseData) {
        try {
            logger.info("Updating course with ID: {}", id);
            Course course = getCourseById(id);
            course.setName(newCourseData.getName());
            course.setCredit(newCourseData.getCredit());
            course.setDescription(newCourseData.getDescription());
            course.setSemester(newCourseData.getSemester());
            Course updatedCourse = courseRepository.save(course);
            logger.info("Course updated successfully: {}", updatedCourse);
            return updatedCourse;
        } catch (Exception e) {
            logger.error("Error updating course with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error updating course: " + e.getMessage(), e);
        }
    }

    public int deleteCourse(Long id) {
        logger.info("Attempting to delete course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Course not found with ID: {}", id);
                    return new ResourceNotFoundException("No course found with ID: " + id);
                });

        int enrollmentCount = course.getEnrollments() != null ? course.getEnrollments().size() : 0;
        logger.info("Course with ID: {} has {} enrollment records that will be deleted", id, enrollmentCount);

        courseRepository.delete(course);
        logger.info("Successfully deleted course with ID: {} and {} enrollment records", id, enrollmentCount);
        return enrollmentCount;
    }

    public List<Student> getStudentsInCourse(Long courseId) {
        logger.info("Fetching students for course ID: {}", courseId);
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<Student> students = enrollments.stream()
                .filter(enrollment -> enrollment.getStudent() != null)
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
        logger.info("Found {} students for course ID: {}", students.size(), courseId);
        return students;
    }
}