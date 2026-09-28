package io.github.homeworkportal.submission;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.course.CourseService;
import io.github.homeworkportal.domain.Assignment;
import io.github.homeworkportal.domain.Submission;
import io.github.homeworkportal.domain.SubmissionVersion;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.SubmissionRepository;
import io.github.homeworkportal.repository.SubmissionVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class SubmissionRecorder {
    private final CourseService courses;
    private final AccountService accounts;
    private final SubmissionRepository submissions;
    private final SubmissionVersionRepository versions;

    public SubmissionRecorder(CourseService courses, AccountService accounts,
                              SubmissionRepository submissions, SubmissionVersionRepository versions) {
        this.courses = courses;
        this.accounts = accounts;
        this.submissions = submissions;
        this.versions = versions;
    }

    @Transactional
    public SubmissionVersion record(Long assignmentId, String username, String originalFilename,
                                    String storageKey, String mediaType, long byteSize, String sha256,
                                    Instant submittedAt, boolean late) {
        Assignment assignment = courses.requireAssignment(assignmentId);
        UserAccount student = accounts.requireByUsername(username);
        Submission submission = submissions.findByAssignment_IdAndStudent_Id(assignmentId, student.getId())
                .orElseGet(() -> submissions.save(new Submission(assignment, student)));
        int versionNumber = submission.getLastVersion() + 1;
        submission.recordVersion(versionNumber, submittedAt);
        submissions.save(submission);
        return versions.save(new SubmissionVersion(submission, versionNumber, originalFilename,
                storageKey, mediaType, byteSize, sha256, submittedAt, late));
    }
}
