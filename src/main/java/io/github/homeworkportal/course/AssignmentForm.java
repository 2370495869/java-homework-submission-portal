package io.github.homeworkportal.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

public class AssignmentForm {
    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueAt;

    private boolean allowLate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public boolean isAllowLate() { return allowLate; }
    public void setAllowLate(boolean allowLate) { this.allowLate = allowLate; }
}
