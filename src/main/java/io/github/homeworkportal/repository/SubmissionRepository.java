package io.github.homeworkportal.repository;

import io.github.homeworkportal.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);
    List<Submission> findByAssignment_IdOrderByStudent_IdAsc(Long assignmentId);
}
