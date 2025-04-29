package com.homework.project3.model;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StudentDTO {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Surname cannot be blank")
    private String surname;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Department cannot be blank")
    private String department;

    private transient String profileImage;

    private transient MultipartFile profileImageFile;

    private List<Long> enrollmentIds;

    public StudentDTO(Long id, String name, String surname, String email, String department, String profileImage, List<Long> enrollmentIds) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.department = department;
        this.profileImage = profileImage;
        this.enrollmentIds = enrollmentIds;
    }
}
