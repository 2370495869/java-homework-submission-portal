package io.github.homeworkportal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_submission_assignment_student", columnNames = {"assignment_id", "student_id"}))
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserAccount student;

    @Column(name = "last_version", nullable = false)
    private int lastVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Submission() {
    }

    public Submission(Assignment assignment, UserAccount student) {
        this.assignment = assignment;
        this.student = student;
        this.lastVersion = 0;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Assignment getAssignment() { return assignment; }
    public UserAccount getStudent() { return student; }
    public int getLastVersion() { return lastVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void recordVersion(int version, Instant at) {
        this.lastVersion = version;
        this.updatedAt = at;
    }
}
