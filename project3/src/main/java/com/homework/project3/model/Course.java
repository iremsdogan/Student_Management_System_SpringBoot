package com.homework.project3.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Column(nullable = false, unique = true)
    private String name;

    @Min(value = 1, message = "Credit must be at least 1")
    @Column(nullable = false)
    private int credit;

    @NotBlank(message = "Description cannot be blank")
    @Column(nullable = false)
    private String description;

    @NotBlank(message = "Semester cannot be blank")
    @Column(nullable = false)
    private String semester;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments;
}
