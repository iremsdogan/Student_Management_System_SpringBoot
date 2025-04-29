package com.homework.project3.controller;

import com.homework.project3.model.Student;
import com.homework.project3.model.Enrollment;
import com.homework.project3.model.StudentDTO;
import com.homework.project3.service.StudentService;
import com.homework.project3.service.FileStorageService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/students")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    public StudentController(StudentService studentService, FileStorageService fileStorageService) {
        this.studentService = studentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String getAllStudents(Model model) {
        try {
            logger.info("Getting all students");
            List<Student> students = studentService.getAllStudents();
            List<StudentDTO> studentDTOs = students.stream().map(StudentController::convertToStudentDTO).collect(Collectors.toList());
            model.addAttribute("students", studentDTOs);
            logger.info("Successfully retrieved {} students", students.size());
        } catch (Exception e) {
            logger.error("Error loading students: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading students: " + e.getMessage());
        }
        return "students";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            logger.info("Showing edit form for student with ID: {}", id);
            Student student = studentService.getStudentById(id);
            model.addAttribute("student", convertToStudentDTO(student));
            logger.info("Successfully loaded student for editing: {}", student);
        } catch (Exception e) {
            logger.error("Error loading student for editing: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading student: " + e.getMessage());
            return "redirect:/students";
        }
        return "edit-student";
    }

    @PostMapping("/edit/{id}")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("student") StudentDTO studentDTO,
                                BindingResult result,
                                @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        logger.info("Attempting to update student with ID: {}", id);

        if (result.hasErrors()) {
            logger.error("Validation errors found: {}", result.getAllErrors());
            model.addAttribute("student", studentDTO);
            return "edit-student";
        }

        try {
            // Get existing student first
            Student existingStudent = studentService.getStudentById(id);
            if (existingStudent == null) {
                logger.error("Student not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Student not found");
                return "redirect:/students";
            }

            // Update student data
            Student student = new Student();
            student.setId(id);
            student.setName(studentDTO.getName());
            student.setSurname(studentDTO.getSurname());
            student.setEmail(studentDTO.getEmail());
            student.setDepartment(studentDTO.getDepartment());

            // Handle profile image
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                logger.info("Processing new profile image upload");
                try {
                    String fileName = fileStorageService.storeFile(profileImageFile);
                    student.setProfileImage(fileName);
                    logger.info("Profile image uploaded successfully: {}", fileName);
                } catch (IOException e) {
                    logger.error("Failed to upload profile image: {}", e.getMessage());
                    student.setProfileImage(existingStudent.getProfileImage());
                    redirectAttributes.addFlashAttribute("warningMessage", "Failed to upload new profile image. Keeping existing one.");
                }
            } else {
                logger.info("No new profile image provided, keeping existing one");
                student.setProfileImage(existingStudent.getProfileImage());
            }

            // Update student
            studentService.updateStudent(id, student);
            logger.info("Student updated successfully");
            redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");

        } catch (Exception e) {
            logger.error("Error updating student: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
            return "redirect:/students/edit/" + id;
        }

        return "redirect:/students";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentById(id);
            int enrollmentCount = student.getEnrollments() != null ? student.getEnrollments().size() : 0;
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Student deleted successfully!", enrollmentCount));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/students";
    }

    @GetMapping("/view/{id}")
    public String viewStudent(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentById(id);
            model.addAttribute("student", convertToStudentDTO(student));
            return "profile"; // profile.html dosyasını kullanacak
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Student not found!");
            return "redirect:/students";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("student", new StudentDTO());
        return "add-student";
    }

    @PostMapping("/add")
    public String addStudent(@Valid @ModelAttribute("student") StudentDTO studentDTO,
                             BindingResult result,
                             @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("student", studentDTO);
            return "add-student";
        }
        try {
            Student student = new Student();
            student.setName(studentDTO.getName());
            student.setSurname(studentDTO.getSurname());
            student.setEmail(studentDTO.getEmail());
            student.setDepartment(studentDTO.getDepartment());
            // Profil resmi yükleme
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                String fileName = fileStorageService.storeFile(profileImageFile);
                student.setProfileImage(fileName);
            }
            studentService.addStudent(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully!");
            return "redirect:/students";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error adding student: " + e.getMessage());
            return "add-student";
        }
    }

    // REST API endpoints
    @RestController
    @RequestMapping("/api/students")
    public static class StudentApiController {
        private final StudentService studentService;
        private final FileStorageService fileStorageService;

        public StudentApiController(StudentService studentService, FileStorageService fileStorageService) {
            this.studentService = studentService;
            this.fileStorageService = fileStorageService;
        }

        @Operation(summary = "Get all students", description = "Retrieve a list of all students")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved list of students"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<List<StudentDTO>> getAllStudents() {
            List<Student> students = studentService.getAllStudents();
            List<StudentDTO> studentDTOs = students.stream().map(StudentController::convertToStudentDTO).collect(Collectors.toList());
            return ResponseEntity.ok(studentDTOs);
        }

        @Operation(summary = "Get a student by ID", description = "Retrieve a specific student by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved the student"),
                @ApiResponse(responseCode = "404", description = "Student not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/{id}")
        public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
            Student student = studentService.getStudentById(id);
            return ResponseEntity.ok(convertToStudentDTO(student));
        }

        @Operation(summary = "Add a new student", description = "Create a new student")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Student created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid input"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping
        public ResponseEntity<StudentDTO> addStudent(@Valid @RequestBody StudentDTO studentDTO) {
            Student student = convertToStudent(studentDTO);
            Student savedStudent = studentService.addStudent(student);
            return ResponseEntity.status(201).body(convertToStudentDTO(savedStudent));
        }

        @Operation(summary = "Update a student by ID", description = "Update an existing student by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Student updated successfully"),
                @ApiResponse(responseCode = "404", description = "Student not found"),
                @ApiResponse(responseCode = "400", description = "Invalid input"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PutMapping("/{id}")
        public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDTO) {
            Student student = convertToStudent(studentDTO);
            Student updatedStudent = studentService.updateStudent(id, student);
            return ResponseEntity.ok(convertToStudentDTO(updatedStudent));
        }

        @Operation(summary = "Delete a student by ID", description = "Delete a student by its ID")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Student deleted successfully"),
                @ApiResponse(responseCode = "404", description = "Student not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
            studentService.deleteStudent(id);
            return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Upload a profile image for a student", description = "Upload a profile image for a specific student")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Profile image uploaded successfully"),
                @ApiResponse(responseCode = "404", description = "Student not found"),
                @ApiResponse(responseCode = "500", description = "Failed to upload file")
        })
        @PostMapping("/{id}/profile-image")
        public ResponseEntity<String> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
            try {
                String fileName = fileStorageService.storeFile(file);
                Student student = studentService.getStudentById(id);
                student.setProfileImage(fileName);
                studentService.updateStudent(id, student);
                String redirectUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/students/edit/{id}")
                        .buildAndExpand(id)
                        .toUriString();
                return ResponseEntity.ok().header("Location", redirectUrl).body(fileName);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
            }
        }
    }

    private static StudentDTO convertToStudentDTO(Student student) {
        List<Long> enrollmentIds = student.getEnrollments() != null ?
                student.getEnrollments().stream().map(Enrollment::getId).collect(Collectors.toList()) : List.of();
        return new StudentDTO(
                student.getId(),
                student.getName(),
                student.getSurname(),
                student.getEmail(),
                student.getDepartment(),
                student.getProfileImage(),
                enrollmentIds
        );
    }

    private static Student convertToStudent(StudentDTO studentDTO) {
        Student student = new Student();
        student.setName(studentDTO.getName());
        student.setSurname(studentDTO.getSurname());
        student.setEmail(studentDTO.getEmail());
        student.setDepartment(studentDTO.getDepartment());
        student.setProfileImage(studentDTO.getProfileImage());
        return student;
    }
}