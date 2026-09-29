package io.github.homeworkportal.submission;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.course.CourseService;
import io.github.homeworkportal.domain.Assignment;
import io.github.homeworkportal.domain.Enrollment;
import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.Submission;
import io.github.homeworkportal.domain.SubmissionVersion;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.EnrollmentRepository;
import io.github.homeworkportal.repository.SubmissionRepository;
import io.github.homeworkportal.repository.SubmissionVersionRepository;
import io.github.homeworkportal.storage.FileStorage;
import io.github.homeworkportal.storage.FileTypeValidator;
import io.github.homeworkportal.storage.StoredObject;
import io.github.homeworkportal.storage.UploadLimits;
import io.github.homeworkportal.storage.ValidatedUpload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SubmissionService {
    private final AccountService accounts;
    private final CourseService courses;
    private final EnrollmentRepository enrollments;
    private final SubmissionRepository submissions;
    private final SubmissionVersionRepository versions;
    private final SubmissionRecorder recorder;
    private final FileStorage storage;

    public SubmissionService(AccountService accounts, CourseService courses,
                             EnrollmentRepository enrollments, SubmissionRepository submissions,
                             SubmissionVersionRepository versions, SubmissionRecorder recorder,
                             FileStorage storage) {
        this.accounts = accounts;
        this.courses = courses;
        this.enrollments = enrollments;
        this.submissions = submissions;
        this.versions = versions;
        this.recorder = recorder;
        this.storage = storage;
    }

    public SubmissionVersion submit(Long assignmentId, String username, MultipartFile file) throws IOException {
        Assignment assignment = courses.requireAssignment(assignmentId);
        UserAccount student = accounts.requireByUsername(username);
        if (student.getRole() != Role.STUDENT
                || !enrollments.existsByCourse_IdAndStudent_Id(assignment.getCourse().getId(), student.getId())) {
            throw new AccessDeniedException("只有本课程的学生可以提交作业。");
        }
        if (file == null || file.isEmpty() || file.getSize() > UploadLimits.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("请选择不超过 10 MiB 的非空文件。");
        }

        ValidatedUpload metadata;
        try (InputStream input = file.getInputStream()) {
            metadata = FileTypeValidator.validate(file.getOriginalFilename(), input);
        }
        Instant submittedAt = Instant.now();
        boolean late = submittedAt.isAfter(assignment.getDueAt());
        if (late && !assignment.isAllowLate()) {
            throw new IllegalArgumentException("截止时间已过，教师未开放迟交。");
        }

        String storageKey = UUID.randomUUID() + metadata.extension();
        StoredObject stored;
        try {
            stored = storage.store(storageKey, file.getInputStream());
            if (stored.size() < 1 || stored.size() > UploadLimits.MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("文件大小不符合限制。");
            }
            return recorder.record(assignmentId, username, metadata.filename(), storageKey,
                    metadata.mediaType(), stored.size(), stored.sha256(), submittedAt, late);
        } catch (IOException | RuntimeException ex) {
            try {
                storage.delete(storageKey);
            } catch (IOException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> forAssignment(Long assignmentId, String username) {
        Assignment assignment = courses.requireAssignment(assignmentId);
        courses.requireVisibleCourse(assignment.getCourse().getId(), username);
        UserAccount caller = accounts.requireByUsername(username);
        List<Submission> matching;
        if (caller.getRole() == Role.ADMIN) {
            matching = submissions.findByAssignment_IdOrderByStudent_IdAsc(assignmentId);
        } else if (caller.getRole() == Role.TEACHER) {
            if (!assignment.getCourse().getTeacher().getId().equals(caller.getId())) {
                throw new AccessDeniedException("你没有查看这份作业提交的权限。");
            }
            matching = submissions.findByAssignment_IdOrderByStudent_IdAsc(assignmentId);
        } else {
            if (!enrollments.existsByCourse_IdAndStudent_Id(assignment.getCourse().getId(), caller.getId())) {
                throw new AccessDeniedException("你没有查看这份作业的权限。");
            }
            matching = submissions.findByAssignment_IdAndStudent_Id(assignmentId, caller.getId())
                    .map(List::of).orElseGet(List::of);
        }

        List<SubmissionSummary> result = new ArrayList<>();
        for (Submission submission : matching) {
            List<SubmissionVersion> history =
                    versions.findBySubmission_IdOrderByVersionNumberDesc(submission.getId());
            result.add(new SubmissionSummary(submission.getStudent().getDisplayName(),
                    submission.getStudent().getUsername(), history));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AuthorizedDownload openAuthorizedVersion(Long versionId, String username) throws IOException {
        SubmissionVersion version = versions.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("文件不存在。"));
        Submission submission = version.getSubmission();
        Assignment assignment = submission.getAssignment();
        UserAccount caller = accounts.requireByUsername(username);
        boolean allowed = switch (caller.getRole()) {
            case ADMIN -> true;
            case TEACHER -> assignment.getCourse().getTeacher().getId().equals(caller.getId());
            case STUDENT -> submission.getStudent().getId().equals(caller.getId());
        };
        if (!allowed) {
            throw new NoSuchElementException("文件不存在。");
        }
        return new AuthorizedDownload(version, storage.open(version.getStorageKey()));
    }
}
