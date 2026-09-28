package io.github.homeworkportal.repository;

import io.github.homeworkportal.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByCourse_IdAndStudent_Id(Long courseId, Long studentId);
    List<Enrollment> findByStudent_IdOrderByJoinedAtDesc(Long studentId);
    List<Enrollment> findByCourse_IdOrderByJoinedAtAsc(Long courseId);
}
