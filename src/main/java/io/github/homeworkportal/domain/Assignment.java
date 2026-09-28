package io.github.homeworkportal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "allow_late", nullable = false)
    private boolean allowLate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Assignment() {
    }

    public Assignment(Course course, String title, String description, Instant dueAt, boolean allowLate) {
        this.course = course;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.allowLate = allowLate;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getDueAt() { return dueAt; }
    public boolean isAllowLate() { return allowLate; }
    public Instant getCreatedAt() { return createdAt; }
}
