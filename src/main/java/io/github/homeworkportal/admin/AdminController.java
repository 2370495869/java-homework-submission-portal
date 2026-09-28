package io.github.homeworkportal.admin;

import io.github.homeworkportal.account.AccountService;
import io.github.homeworkportal.domain.UserAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AccountService accounts;

    public AdminController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/admin/users")
    public String users(Authentication authentication, Model model) {
        model.addAttribute("me", accounts.requireByUsername(authentication.getName()));
        model.addAttribute("users", accounts.listUsers());
        return "admin-users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam String role,
                             RedirectAttributes redirect) {
        try {
            accounts.changeRole(id, role);
            redirect.addFlashAttribute("notice", "用户角色已更新。");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
