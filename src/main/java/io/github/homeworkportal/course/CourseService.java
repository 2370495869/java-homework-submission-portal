package io.github.homeworkportal.course;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.domain.Assignment;
import io.github.homeworkportal.domain.Course;
import io.github.homeworkportal.domain.Enrollment;
import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.AssignmentRepository;
import io.github.homeworkportal.repository.CourseRepository;
import io.github.homeworkportal.repository.EnrollmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CourseService {
    private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountService accounts;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;

    public CourseService(AccountService accounts, CourseRepository courses,
                         EnrollmentRepository enrollments, AssignmentRepository assignments) {
        this.accounts = accounts;
        this.courses = courses;
        this.enrollments = enrollments;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<Course> listVisibleCourses(String username) {
        UserAccount user = accounts.requireByUsername(username);
        if (user.getRole() == Role.ADMIN) {
            return courses.findAll(org.springframework.data.domain.Sort.by("createdAt").descending());
        }
        if (user.getRole() == Role.TEACHER) {
            return courses.findByTeacher_IdOrderByCreatedAtDesc(user.getId());
        }
        List<Course> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments.findByStudent_IdOrderByJoinedAtDesc(user.getId())) {
            Course course = enrollment.getCourse();
            course.getTitle();
            course.getDescription();
            result.add(course);
        }
        return result;
    }

    @Transactional
    public Course createCourse(String username, String title, String description) {
        UserAccount teacher = accounts.requireByUsername(username);
        if (teacher.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("教师角色为创建课程所必需。");
        }
        String cleanTitle = requireText(title, 120, "课程名称");
        String cleanDescription = cleanOptionalText(description, 2000);
        for (int attempt = 0; attempt < 5; attempt++) {
            String inviteCode = newInviteCode();
            if (courses.findByInviteCode(inviteCode).isEmpty()) {
                return courses.save(new Course(cleanTitle, cleanDescription, teacher, inviteCode));
            }
        }
        throw new IllegalStateException("无法生成课程邀请码，请重试。");
    }

    @Transactional
    public Course joinCourse(String username, String suppliedCode) {
        UserAccount student = accounts.requireByUsername(username);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("只有学生账户可以加入课程。");
        }
        String code = suppliedCode == null ? "" : suppliedCode.trim().toUpperCase(Locale.ROOT);
        Course course = courses.findByInviteCode(code)
                .orElseThrow(() -> new IllegalArgumentException("邀请码无效，请向教师确认。"));
        if (!enrollments.existsByCourse_IdAndStudent_Id(course.getId(), student.getId())) {
            enrollments.save(new Enrollment(course, student));
        }
        return course;
    }

    @Transactional(readOnly = true)
    public Course requireVisibleCourse(Long courseId, String username) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new java.util.NoSuchElementException("找不到这门课程。"));
        UserAccount user = accounts.requireByUsername(username);
        boolean visible = switch (user.getRole()) {
            case ADMIN -> true;
            case TEACHER -> course.getTeacher().getId().equals(user.getId());
            case STUDENT -> enrollments.existsByCourse_IdAndStudent_Id(courseId, user.getId());
        };
        if (!visible) {
            throw new AccessDeniedException("你没有查看这门课程的权限。");
        }
        return course;
    }

    @Transactional
    public Assignment createAssignment(String username, Long courseId, String title, String description,
                                       Instant dueAt, boolean allowLate) {
        Course course = requireVisibleCourse(courseId, username);
        UserAccount user = accounts.requireByUsername(username);
        if (user.getRole() != Role.TEACHER || !course.getTeacher().getId().equals(user.getId())) {
            throw new AccessDeniedException("只有本课程的教师可以发布作业。");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("请填写截止时间。");
        }
        return assignments.save(new Assignment(course, requireText(title, 120, "作业名称"),
                cleanOptionalText(description, 4000), dueAt, allowLate));
    }

    @Transactional(readOnly = true)
    public List<Assignment> assignmentsFor(Long courseId, String username) {
        requireVisibleCourse(courseId, username);
        return assignments.findByCourse_IdOrderByDueAtAsc(courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> rosterForTeacher(Long courseId, String username) {
        Course course = requireVisibleCourse(courseId, username);
        UserAccount user = accounts.requireByUsername(username);
        if (user.getRole() != Role.TEACHER || !course.getTeacher().getId().equals(user.getId())) {
            throw new AccessDeniedException("只有本课程的教师可以查看学生名单。");
        }
        return enrollments.findByCourse_IdOrderByJoinedAtAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Assignment requireAssignment(Long assignmentId) {
        return assignments.findById(assignmentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("找不到这份作业。"));
    }

    @Transactional(readOnly = true)
    public List<CourseRosterEntry> rosterEntriesForTeacher(Long courseId, String username) {
        Course course = requireVisibleCourse(courseId, username);
        UserAccount user = accounts.requireByUsername(username);
        if (user.getRole() != Role.TEACHER || !course.getTeacher().getId().equals(user.getId())) {
            throw new AccessDeniedException("只有本课程的教师可以查看学生名单。");
        }
        return enrollments.findByCourse_IdOrderByJoinedAtAsc(courseId).stream()
                .map(entry -> new CourseRosterEntry(entry.getStudent().getDisplayName(),
                        entry.getStudent().getUsername(), entry.getJoinedAt()))
                .toList();
    }
    private static String newInviteCode() {
        char[] result = new char[8];
        for (int i = 0; i < result.length; i++) {
            result[i] = INVITE_ALPHABET[RANDOM.nextInt(INVITE_ALPHABET.length)];
        }
        return new String(result);
    }

    private static String requireText(String input, int max, String label) {
        String value = input == null ? "" : input.trim();
        if (value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(label + "不能为空且不能超过 " + max + " 个字符。");
        }
        return value;
    }

    private static String cleanOptionalText(String input, int max) {
        String value = input == null ? "" : input.trim();
        if (value.length() > max) {
            throw new IllegalArgumentException("说明最多 " + max + " 个字符。");
        }
        return value;
    }
}
