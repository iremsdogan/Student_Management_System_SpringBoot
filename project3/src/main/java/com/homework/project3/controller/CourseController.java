package com.homework.project3.controller;

import com.homework.project3.model.Course;
import com.homework.project3.model.CourseDTO;
import com.homework.project3.model.Enrollment;
import com.homework.project3.model.Student;
import com.homework.project3.model.StudentDTO;
import com.homework.project3.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // Endpoints for HTML pages
    @GetMapping
    public String showCoursesPage(Model model) {
        try {
            logger.info("Getting all courses");
            List<Course> courses = courseService.getAllCourses();
            List<CourseDTO> courseDTOs = courses.stream().map(CourseController::convertToCourseDTO).collect(Collectors.toList());
            model.addAttribute("courses", courseDTOs);
            logger.info("Successfully retrieved {} courses", courses.size());
        } catch (Exception e) {
            logger.error("Error loading courses: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading courses: " + e.getMessage());
        }
        return "courses";
    }

    @GetMapping("/edit/{id}")
    public String showEditCoursePage(@PathVariable Long id, Model model) {
        try {
            logger.info("Showing edit form for course with ID: {}", id);
            Course course = courseService.getCourseById(id);
            model.addAttribute("course", convertToCourseDTO(course));
            logger.info("Successfully loaded course for editing: {}", course);
        } catch (Exception e) {
            logger.error("Error loading course for editing: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading course: " + e.getMessage());
            return "redirect:/courses";
        }
        return "edit-course";
    }

    @PostMapping("/edit/{id}")
    public String updateCourse(@PathVariable Long id,
                               @Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        logger.info("Attempting to update course with ID: {}", id);

        if (result.hasErrors()) {
            logger.error("Validation errors found: {}", result.getAllErrors());
            model.addAttribute("course", courseDTO);
            return "edit-course";
        }

        try {
            // Get existing course first
            Course existingCourse = courseService.getCourseById(id);
            if (existingCourse == null) {
                logger.error("Course not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Course not found");
                return "redirect:/courses";
            }

            // Update course data
            Course course = convertToCourse(courseDTO);
            course.setId(id);
            courseService.updateCourse(id, course);
            logger.info("Course updated successfully");
            redirectAttributes.addFlashAttribute("successMessage", "Course updated successfully!");

        } catch (Exception e) {
            logger.error("Error updating course: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating course: " + e.getMessage());
            return "redirect:/courses/edit/" + id;
        }

        return "redirect:/courses";
    }

    @GetMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseById(id);
            int enrollmentCount = course.getEnrollments() != null ? course.getEnrollments().size() : 0;
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("successMessage",
                String.format("Kurs ve ilişkili %d kayıt başarıyla silindi!", enrollmentCount));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting course: " + e.getMessage());
        }
        return "redirect:/courses";
    }

    @GetMapping("/view/{id}")
    public String viewCourse(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseById(id);
            model.addAttribute("course", convertToCourseDTO(course));
            return "view-course"; // Assume a view-course.html exists
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found!");
            return "redirect:/courses";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("course", new CourseDTO());
        return "add-course";
    }

    @PostMapping("/add")
    public String addCourse(@Valid @ModelAttribute("course") CourseDTO courseDTO,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("course", courseDTO);
            return "add-course";
        }
        try {
            Course course = new Course();
            course.setName(courseDTO.getName());
            course.setCredit(courseDTO.getCredit());
            course.setDescription(courseDTO.getDescription());
            course.setSemester(courseDTO.getSemester());

            courseService.addCourse(course);
            redirectAttributes.addFlashAttribute("successMessage", "Course added successfully!");
            return "redirect:/courses";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error adding course: " + e.getMessage());
            return "add-course";
        }
    }

    // REST API Endpoints
    @RestController
    @RequestMapping("/api/courses")
    public static class CourseApiController {

        private final CourseService courseService;

        public CourseApiController(CourseService courseService) {
            this.courseService = courseService;
        }

        @Operation(summary = "Get all courses", description = "Retrieve a list of all courses")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved list of courses"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<List<CourseDTO>> getAllCourses() {
            List<Course> courses = courseService.getAllCourses();
            List<CourseDTO> courseDTOs = courses.stream().map(CourseController::convertToCourseDTO).collect(Collectors.toList());
            return ResponseEntity.ok(courseDTOs);
        }

        @Operation(summary = "Get a course by ID", description = "Retrieve a specific course by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved the course"),
                @ApiResponse(responseCode = "404", description = "Course not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/{id}")
        public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
            Course course = courseService.getCourseById(id);
            return ResponseEntity.ok(convertToCourseDTO(course));
        }

        @Operation(summary = "Add a new course", description = "Create a new course")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Course created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid input"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping
        public ResponseEntity<CourseDTO> addCourse(@Valid @RequestBody CourseDTO courseDTO) {
            Course course = convertToCourse(courseDTO);
            Course savedCourse = courseService.addCourse(course);
            return ResponseEntity.status(201).body(convertToCourseDTO(savedCourse));
        }

        @Operation(summary = "Update a course by ID", description = "Update an existing course by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Course updated successfully"),
                @ApiResponse(responseCode = "404", description = "Course not found"),
                @ApiResponse(responseCode = "400", description = "Invalid input"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PutMapping("/{id}")
        public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseDTO courseDTO) {
            Course course = convertToCourse(courseDTO);
            Course updatedCourse = courseService.updateCourse(id, course);
            return ResponseEntity.ok(convertToCourseDTO(updatedCourse));
        }

        @Operation(summary = "Delete a course by ID", description = "Delete a course by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
                @ApiResponse(responseCode = "404", description = "Course not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Get students in a course", description = "Retrieve all students enrolled in a specific course")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved list of students"),
                @ApiResponse(responseCode = "404", description = "Course not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/{courseId}/students")
        public ResponseEntity<List<StudentDTO>> getStudentsInCourse(@PathVariable Long courseId) {
            List<Student> students = courseService.getStudentsInCourse(courseId);
            List<StudentDTO> studentDTOs = students.stream().map(CourseController::convertToStudentDTO).collect(Collectors.toList());
            return ResponseEntity.ok(studentDTOs);
        }
    }

    private static CourseDTO convertToCourseDTO(Course course) {
        List<Long> enrollmentIds = course.getEnrollments().stream().map(Enrollment::getId).collect(Collectors.toList());
        return new CourseDTO(course.getId(), course.getName(), course.getCredit(), course.getDescription(), course.getSemester(), enrollmentIds);
    }

    private static Course convertToCourse(CourseDTO courseDTO) {
        Course course = new Course();
        course.setId(courseDTO.getId());
        course.setName(courseDTO.getName());
        course.setCredit(courseDTO.getCredit());
        course.setDescription(courseDTO.getDescription());
        course.setSemester(courseDTO.getSemester());
        return course;
    }

    private static StudentDTO convertToStudentDTO(Student student) {
        List<Long> enrollmentIds = student.getEnrollments() != null ?
                student.getEnrollments().stream().map(Enrollment::getId).collect(Collectors.toList()) : List.of();
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(student.getId());
        studentDTO.setName(student.getName());
        studentDTO.setSurname(student.getSurname());
        studentDTO.setEmail(student.getEmail());
        studentDTO.setDepartment(student.getDepartment());
        studentDTO.setProfileImage(student.getProfileImage());
        studentDTO.setEnrollmentIds(enrollmentIds);
        return studentDTO;
    }
}