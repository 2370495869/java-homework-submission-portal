package io.github.homeworkportal.repository;

import io.github.homeworkportal.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByInviteCode(String inviteCode);
    List<Course> findByTeacher_IdOrderByCreatedAtDesc(Long teacherId);
}
