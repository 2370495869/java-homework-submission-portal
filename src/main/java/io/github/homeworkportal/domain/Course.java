package io.github.homeworkportal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private UserAccount teacher;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Course() {
    }

    public Course(String title, String description, UserAccount teacher, String inviteCode) {
        this.title = title;
        this.description = description;
        this.teacher = teacher;
        this.inviteCode = inviteCode;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UserAccount getTeacher() { return teacher; }
    public String getInviteCode() { return inviteCode; }
    public Instant getCreatedAt() { return createdAt; }
}
