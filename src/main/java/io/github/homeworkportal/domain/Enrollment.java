package io.github.homeworkportal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uq_enrollment_course_student", columnNames = {"course_id", "student_id"}))
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserAccount student;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected Enrollment() {
    }

    public Enrollment(Course course, UserAccount student) {
        this.course = course;
        this.student = student;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public UserAccount getStudent() { return student; }
    public Instant getJoinedAt() { return joinedAt; }
}
