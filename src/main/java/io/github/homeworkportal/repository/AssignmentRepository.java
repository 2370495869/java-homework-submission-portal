package io.github.homeworkportal.repository;

import io.github.homeworkportal.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourse_IdOrderByDueAtAsc(Long courseId);
}
