package com.homework.project3.controller;

import com.homework.project3.model.Course;
import com.homework.project3.model.Enrollment;
import com.homework.project3.model.EnrollmentDTO;
import com.homework.project3.model.Student;
import com.homework.project3.service.EnrollmentService;
import com.homework.project3.service.StudentService;
import com.homework.project3.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final CourseService courseService;

    public EnrollmentController(EnrollmentService enrollmentService, StudentService studentService, CourseService courseService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.courseService = courseService;
    }

    // HTML Sayfaları için Endpoint'ler
    @GetMapping
    public String showEnrollmentsPage(Model model) {
        try {
            model.addAttribute("enrollments", enrollmentService.getAllEnrollments());
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("courses", courseService.getAllCourses());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading enrollments: " + e.getMessage());
        }
        return "enrollments";
    }

    @GetMapping("/edit/{id}")
    public String showEditEnrollmentPage(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("enrollment", enrollmentService.getEnrollmentById(id));
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("courses", courseService.getAllCourses());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading enrollment: " + e.getMessage());
            return "redirect:/enrollments";
        }
        return "edit-enrollment";
    }

    @PostMapping("/edit/{id}")
    public String updateEnrollment(@PathVariable Long id, @Valid @ModelAttribute("enrollment") EnrollmentDTO enrollmentDTO,
                                   BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "edit-enrollment";
        }
        try {
            Enrollment updatedEnrollment = convertToEnrollment(enrollmentDTO);
            enrollmentService.updateEnrollment(id, updatedEnrollment);
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update enrollment: " + e.getMessage());
            return "redirect:/enrollments/edit/" + id;
        }
        return "redirect:/enrollments";
    }

    @GetMapping("/view/{id}")
    public String showViewEnrollmentPage(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("enrollment", enrollmentService.getEnrollmentById(id));
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading enrollment: " + e.getMessage());
            return "redirect:/enrollments";
        }
        return "view-enrollment";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        try {
            model.addAttribute("enrollment", new EnrollmentDTO());
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("courses", courseService.getAllCourses());
            return "add-enrollment";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            return "redirect:/enrollments";
        }
    }

    @PostMapping("/add")
    public String addEnrollment(@Valid @ModelAttribute("enrollment") EnrollmentDTO enrollmentDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            try {
                model.addAttribute("students", studentService.getAllStudents());
                model.addAttribute("courses", courseService.getAllCourses());
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error loading form data: " + e.getMessage());
            }
            return "add-enrollment";
        }

        try {
            Enrollment enrollment = convertToEnrollment(enrollmentDTO);
            enrollmentService.createEnrollment(enrollmentDTO.getCourseId(),
                    enrollmentDTO.getStudentId(),
                    enrollment);
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment added successfully!");
            return "redirect:/enrollments";
        } catch (Exception e) {
            try {
                model.addAttribute("students", studentService.getAllStudents());
                model.addAttribute("courses", courseService.getAllCourses());
            } catch (Exception ex) {
                model.addAttribute("errorMessage", "Error loading form data: " + ex.getMessage());
            }
            model.addAttribute("errorMessage", "Error adding enrollment: " + e.getMessage());
            return "add-enrollment";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteEnrollment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.deleteEnrollment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting enrollment: " + e.getMessage());
        }
        return "redirect:/enrollments";
    }

    // REST API Endpoints
    @RestController
    @RequestMapping("/api/enrollments")
    public static class EnrollmentApiController {

        private final EnrollmentService enrollmentService;

        public EnrollmentApiController(EnrollmentService enrollmentService) {
            this.enrollmentService = enrollmentService;
        }

        @Operation(summary = "Get all enrollments", description = "Retrieve all enrollments")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved list of enrollments"),
                @ApiResponse(responseCode = "500", description = "Server error")
        })
        @GetMapping
        public ResponseEntity<List<EnrollmentDTO>> getAllEnrollments() {
            List<Enrollment> enrollments = enrollmentService.getAllEnrollments();
            List<EnrollmentDTO> enrollmentDTOs = enrollments.stream().map(EnrollmentController::convertToEnrollmentDTO).collect(Collectors.toList());
            return ResponseEntity.ok(enrollmentDTOs);
        }

        @Operation(summary = "Get a specific record", description = "Retrieves a specific record by ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Enrollment successfully retrieved"),
                @ApiResponse(responseCode = "404", description = "No enrollment found"),
                @ApiResponse(responseCode = "500", description = "Server error")
        })
        @GetMapping("/{id}")
        public ResponseEntity<EnrollmentDTO> getEnrollmentById(@PathVariable Long id) {
            Enrollment enrollment = enrollmentService.getEnrollmentById(id);
            return ResponseEntity.ok(convertToEnrollmentDTO(enrollment));
        }

        @Operation(summary = "Create a new enrollment", description = "Enrolls a student in a course")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Enrollment successfully retrieved"),
                @ApiResponse(responseCode = "404", description = "No courses or students found"),
                @ApiResponse(responseCode = "400", description = "Student is already enrolled in this course"),
                @ApiResponse(responseCode = "500", description = "Server error")
        })
        @PostMapping
        public ResponseEntity<EnrollmentDTO> createEnrollment(@RequestBody EnrollmentDTO enrollmentDTO) {
            Enrollment enrollmentDetails = convertToEnrollment(enrollmentDTO);
            Enrollment enrollment = enrollmentService.createEnrollment(enrollmentDTO.getCourseId(), enrollmentDTO.getStudentId(), enrollmentDetails);
            return ResponseEntity.status(201).body(convertToEnrollmentDTO(enrollment));
        }

        @Operation(summary = "Update a enrollment by ID", description = "Updates information for a specific enrollment")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Enrollment successfully retrieved"),
                @ApiResponse(responseCode = "404", description = "No enrollment found"),
                @ApiResponse(responseCode = "500", description = "Server error")
        })
        @PutMapping("/{id}")
        public ResponseEntity<EnrollmentDTO> updateEnrollment(@PathVariable Long id, @RequestBody EnrollmentDTO enrollmentDTO) {
            Enrollment updatedEnrollment = convertToEnrollment(enrollmentDTO);
            Enrollment enrollment = enrollmentService.updateEnrollment(id, updatedEnrollment);
            return ResponseEntity.ok(convertToEnrollmentDTO(enrollment));
        }

        @Operation(summary = "Delete an enrollment", description = "Deletes a specific enrollment")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Enrollment successfully deleted"),
                @ApiResponse(responseCode = "404", description = "No enrollment found"),
                @ApiResponse(responseCode = "500", description = "Server error")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
            enrollmentService.deleteEnrollment(id);
            return ResponseEntity.noContent().build();
        }
    }

    private static EnrollmentDTO convertToEnrollmentDTO(Enrollment enrollment) {
        return new EnrollmentDTO(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourse().getId(),
                enrollment.getClassDate(),
                enrollment.getTuition(),
                enrollment.isAttendance()
        );
    }

    private static Enrollment convertToEnrollment(EnrollmentDTO enrollmentDTO) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentDTO.getId());
        enrollment.setClassDate(enrollmentDTO.getClassDate());
        enrollment.setTuition(enrollmentDTO.getTuition());
        enrollment.setAttendance(enrollmentDTO.isAttendance());

        Student student = new Student();
        student.setId(enrollmentDTO.getStudentId());
        enrollment.setStudent(student);

        Course course = new Course();
        course.setId(enrollmentDTO.getCourseId());
        enrollment.setCourse(course);

        return enrollment;
    }
}