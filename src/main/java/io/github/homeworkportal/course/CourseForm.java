package io.github.homeworkportal.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseForm {
    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 2000)
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
