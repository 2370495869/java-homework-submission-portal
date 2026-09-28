package io.github.homeworkportal.web;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.course.CourseForm;
import io.github.homeworkportal.course.CourseService;
import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final AccountService accounts;
    private final CourseService courses;

    public DashboardController(AccountService accounts, CourseService courses) {
        this.accounts = accounts;
        this.courses = courses;
    }

    @GetMapping("/")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        UserAccount user = accounts.requireByUsername(authentication.getName());
        model.addAttribute("me", user);
        model.addAttribute("courses", courses.listVisibleCourses(user.getUsername()));
        model.addAttribute("isTeacher", user.getRole() == Role.TEACHER);
        model.addAttribute("isStudent", user.getRole() == Role.STUDENT);
        model.addAttribute("isAdmin", user.getRole() == Role.ADMIN);
        model.addAttribute("courseForm", new CourseForm());
        return "dashboard";
    }
}
