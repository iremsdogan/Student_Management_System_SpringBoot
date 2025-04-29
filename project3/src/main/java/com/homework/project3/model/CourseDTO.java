package com.homework.project3.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Min(value = 1, message = "Credit must be at least 1")
    private int credit;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotBlank(message = "Semester cannot be blank")
    private String semester;

    private List<Long> enrollmentIds;


}
