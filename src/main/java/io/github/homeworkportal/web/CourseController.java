package io.github.homeworkportal.web;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.course.AssignmentForm;
import io.github.homeworkportal.course.CourseForm;
import io.github.homeworkportal.course.CourseService;
import io.github.homeworkportal.course.CourseRosterEntry;
import io.github.homeworkportal.domain.Assignment;
import io.github.homeworkportal.domain.Course;
import io.github.homeworkportal.domain.Enrollment;
import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.SubmissionVersion;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.submission.AuthorizedDownload;
import io.github.homeworkportal.submission.SubmissionService;
import io.github.homeworkportal.submission.SubmissionSummary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CourseController {
    private final AccountService accounts;
    private final CourseService courses;
    private final SubmissionService submissions;
    private final PortalTime time;

    public CourseController(AccountService accounts, CourseService courses,
                            SubmissionService submissions, PortalTime time) {
        this.accounts = accounts;
        this.courses = courses;
        this.submissions = submissions;
        this.time = time;
    }

    @PostMapping("/courses")
    public String createCourse(@Valid @ModelAttribute("courseForm") CourseForm form,
                               BindingResult binding, Authentication authentication,
                               RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "请填写有效的课程名称和说明。");
            return "redirect:/";
        }
        try {
            Course course = courses.createCourse(authentication.getName(), form.getTitle(), form.getDescription());
            redirect.addFlashAttribute("notice", "课程已创建。请把邀请码分享给学生。");
            return "redirect:/courses/" + course.getId();
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/courses/join")
    public String joinCourse(@RequestParam String inviteCode, Authentication authentication,
                             RedirectAttributes redirect) {
        try {
            Course course = courses.joinCourse(authentication.getName(), inviteCode);
            redirect.addFlashAttribute("notice", "已加入课程。");
            return "redirect:/courses/" + course.getId();
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/courses/{courseId}")
    public String course(@PathVariable Long courseId, Authentication authentication, Model model) {
        String username = authentication.getName();
        UserAccount user = accounts.requireByUsername(username);
        Course course = courses.requireVisibleCourse(courseId, username);
        boolean teacher = user.getRole() == Role.TEACHER;
        boolean admin = user.getRole() == Role.ADMIN;

        List<AssignmentPage> assignmentPages = new ArrayList<>();
        for (Assignment assignment : courses.assignmentsFor(courseId, username)) {
            List<SubmissionPage> submissionPages = new ArrayList<>();
            for (SubmissionSummary summary : submissions.forAssignment(assignment.getId(), username)) {
                List<VersionPage> versionPages = summary.versions().stream()
                        .map(this::toVersionPage).toList();
                submissionPages.add(new SubmissionPage(summary.displayName(), summary.username(), versionPages));
            }
            assignmentPages.add(new AssignmentPage(assignment, time.format(assignment.getDueAt()), submissionPages,
                    assignment.isAllowLate() || Instant.now().isBefore(assignment.getDueAt())));
        }

        List<CourseRosterEntry> roster = teacher ? courses.rosterEntriesForTeacher(courseId, username) : List.of();
        model.addAttribute("me", user);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignmentPages);
        model.addAttribute("roster", roster);
        model.addAttribute("isTeacher", teacher);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isStudent", user.getRole() == Role.STUDENT);
        model.addAttribute("assignmentForm", new AssignmentForm());
        return "course";
    }

    @PostMapping("/courses/{courseId}/assignments")
    public String createAssignment(@PathVariable Long courseId,
                                   @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
                                   BindingResult binding, Authentication authentication,
                                   RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "请填写作业名称和有效的截止时间。");
            return "redirect:/courses/" + courseId;
        }
        try {
            courses.createAssignment(authentication.getName(), courseId, form.getTitle(),
                    form.getDescription(), time.toInstant(form.getDueAt()), form.isAllowLate());
            redirect.addFlashAttribute("notice", "作业已发布。");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public String submit(@PathVariable Long assignmentId, @RequestParam("file") MultipartFile file,
                         Authentication authentication, RedirectAttributes redirect) throws IOException {
        Assignment assignment = courses.requireAssignment(assignmentId);
        submissions.submit(assignmentId, authentication.getName(), file);
        redirect.addFlashAttribute("notice", "文件已提交，新版本已记录。");
        return "redirect:/courses/" + assignment.getCourse().getId();
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long versionId,
                                                        Authentication authentication) throws IOException {
        AuthorizedDownload download = submissions.openAuthorizedVersion(versionId, authentication.getName());
        SubmissionVersion version = download.version();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(version.getOriginalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(version.getByteSize())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(download.input()));
    }

    private VersionPage toVersionPage(SubmissionVersion version) {
        return new VersionPage(version.getId(), version.getVersionNumber(),
                version.getOriginalFilename(), time.format(version.getSubmittedAt()),
                version.isLate(), version.getByteSize(), version.getSha256());
    }
}
