package io.github.homeworkportal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_submission_version", columnNames = {"submission_id", "version_number"}))
public class SubmissionVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "original_filename", nullable = false, length = 180)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 50)
    private String storageKey;

    @Column(name = "media_type", nullable = false, length = 100)
    private String mediaType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "late", nullable = false)
    private boolean late;

    protected SubmissionVersion() {
    }

    public SubmissionVersion(Submission submission, int versionNumber, String originalFilename,
                             String storageKey, String mediaType, long byteSize, String sha256,
                             Instant submittedAt, boolean late) {
        this.submission = submission;
        this.versionNumber = versionNumber;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.mediaType = mediaType;
        this.byteSize = byteSize;
        this.sha256 = sha256;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    public Long getId() { return id; }
    public Submission getSubmission() { return submission; }
    public int getVersionNumber() { return versionNumber; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public String getMediaType() { return mediaType; }
    public long getByteSize() { return byteSize; }
    public String getSha256() { return sha256; }
    public Instant getSubmittedAt() { return submittedAt; }
    public boolean isLate() { return late; }
}
