package io.github.homeworkportal.account;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            accounts.registerStudent(form);
            redirect.addFlashAttribute("notice", "学生账户已创建，请登录。");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("username", "username.taken", ex.getMessage());
            model.addAttribute("formError", ex.getMessage());
            return "register";
        }
    }
}
