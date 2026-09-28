package io.github.homeworkportal.repository;

import io.github.homeworkportal.domain.SubmissionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, Long> {
    List<SubmissionVersion> findBySubmission_IdOrderByVersionNumberDesc(Long submissionId);
}
