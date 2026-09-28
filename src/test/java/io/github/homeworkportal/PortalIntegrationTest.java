package io.github.homeworkportal;

import io.github.homeworkportal.course.CourseService;
import io.github.homeworkportal.domain.Assignment;
import io.github.homeworkportal.domain.Course;
import io.github.homeworkportal.domain.Enrollment;
import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.SubmissionVersion;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.AssignmentRepository;
import io.github.homeworkportal.repository.CourseRepository;
import io.github.homeworkportal.repository.EnrollmentRepository;
import io.github.homeworkportal.repository.SubmissionRepository;
import io.github.homeworkportal.repository.SubmissionVersionRepository;
import io.github.homeworkportal.repository.UserAccountRepository;
import io.github.homeworkportal.submission.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalIntegrationTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository users;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;
    @Autowired AssignmentRepository assignments;
    @Autowired SubmissionRepository submissions;
    @Autowired SubmissionVersionRepository versions;
    @Autowired PasswordEncoder encoder;
    @Autowired CourseService courseService;
    @Autowired SubmissionService submissionService;

    @BeforeEach
    void clearDatabase() {
        versions.deleteAll();
        submissions.deleteAll();
        assignments.deleteAll();
        enrollments.deleteAll();
        courses.deleteAll();
        users.deleteAll();
    }

    @Test
    void flywaySchemaExistsAndPublicLoginPagesRender() throws Exception {
        assertTrue(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='user_accounts'", Integer.class) > 0);
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("登录")));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("创建学生账户")));
    }

    @Test
    void registrationCreatesOnlyAStudentWithHashedPasswordAndCsrfIsRequired() throws Exception {
        mvc.perform(post("/register")
                .param("username", "new.student")
                .param("displayName", "测试同学")
                .param("password", "long-unique-passphrase")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        UserAccount student = users.findByUsername("new.student").orElseThrow();
        assertEquals(Role.STUDENT, student.getRole());
        assertTrue(encoder.matches("long-unique-passphrase", student.getPasswordHash()));
        assertNotEquals("long-unique-passphrase", student.getPasswordHash());

        mvc.perform(post("/login").param("username", "new.student")
                        .param("password", "long-unique-passphrase").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("new.student"));

        mvc.perform(post("/register")
                .param("username", "without.csrf")
                .param("displayName", "测试同学")
                .param("password", "long-unique-passphrase"))
            .andExpect(status().isForbidden());
    }

    @Test
    void roleRoutesRejectStudents() throws Exception {
        mvc.perform(get("/admin/users").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/courses").with(user("student").roles("STUDENT")).with(csrf())
                .param("title", "Unauthorized course"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedDashboardsAndCoursePagesRender() throws Exception {
        UserAccount admin = users.save(new UserAccount("platform.admin", "平台管理员",
                encoder.encode("admin-password"), Role.ADMIN));
        mvc.perform(get("/admin/users").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("用户管理")));
        mvc.perform(get("/").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("管理教师权限")));

        UserAccount teacher = users.save(new UserAccount("teacher", "教师",
                encoder.encode("teacher-password"), Role.TEACHER));
        Course course = courseService.createCourse(teacher.getUsername(), "测试课程", "课程说明");
        mvc.perform(get("/").with(user(teacher.getUsername()).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("测试课程")));
        mvc.perform(get("/courses/" + course.getId()).with(user(teacher.getUsername()).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(course.getInviteCode())));
    }

    @Test
    void courseInviteLatePolicyVersionsAndDownloadOwnershipWork() throws Exception {
        UserAccount teacher = users.save(new UserAccount("teacher", "教师", encoder.encode("teacher-password"), Role.TEACHER));
        UserAccount student = users.save(new UserAccount("student", "学生甲", encoder.encode("student-password"), Role.STUDENT));
        UserAccount stranger = users.save(new UserAccount("stranger", "学生乙", encoder.encode("stranger-password"), Role.STUDENT));

        Course course = courseService.createCourse(teacher.getUsername(), "数据结构", "课程说明");
        assertEquals(course.getId(), courseService.joinCourse(student.getUsername(), course.getInviteCode()).getId());
        assertThrows(AccessDeniedException.class, () -> courseService.createCourse(student.getUsername(), "越权", ""));

        Assignment closed = courseService.createAssignment(teacher.getUsername(), course.getId(), "按时提交", "",
                Instant.now().minusSeconds(60), false);
        MockMultipartFile pdf = pdf("homework.pdf");
        assertThrows(IllegalArgumentException.class, () -> submissionService.submit(closed.getId(), student.getUsername(), pdf));

        Assignment open = courseService.createAssignment(teacher.getUsername(), course.getId(), "允许迟交", "",
                Instant.now().minusSeconds(60), true);
        assertThrows(AccessDeniedException.class, () -> submissionService.submit(open.getId(), stranger.getUsername(), pdf("x.pdf")));
        SubmissionVersion first = submissionService.submit(open.getId(), student.getUsername(), pdf("first.pdf"));
        SubmissionVersion second = submissionService.submit(open.getId(), student.getUsername(), pdf("second.pdf"));
        assertTrue(first.isLate());
        assertEquals(1, first.getVersionNumber());
        assertEquals(2, second.getVersionNumber());
        assertEquals(2, submissionService.forAssignment(open.getId(), student.getUsername()).getFirst().versions().size());

        assertThrows(NoSuchElementException.class,
                () -> submissionService.openAuthorizedVersion(first.getId(), stranger.getUsername()));
        var authorized = submissionService.openAuthorizedVersion(first.getId(), teacher.getUsername());
        try (var input = authorized.input()) {
            assertTrue(new String(input.readAllBytes(), StandardCharsets.US_ASCII).startsWith("%PDF-"));
        }
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.7\nexample submission".getBytes(StandardCharsets.US_ASCII));
    }
}
