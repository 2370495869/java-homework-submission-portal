package io.github.homeworkportal.account;

import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class AccountService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount registerStudent(RegistrationForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("这个用户名已被使用。");
        }
        String password = form.getPassword();
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes > 72) {
            throw new IllegalArgumentException("密码转换为 UTF-8 后不能超过 72 字节。");
        }
        String displayName = form.getDisplayName().trim();
        if (displayName.isBlank()) {
            displayName = username;
        }
        UserAccount account = new UserAccount(username, displayName,
                passwordEncoder.encode(password), Role.STUDENT);
        return users.save(account);
    }

    @Transactional(readOnly = true)
    public UserAccount requireByUsername(String username) {
        return users.findByUsername(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("找不到这个账户。"));
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listUsers() {
        return users.findAll(org.springframework.data.domain.Sort.by("createdAt").descending());
    }

    @Transactional
    public void changeRole(Long userId, String requestedRole) {
        Role role;
        try {
            role = Role.valueOf(requestedRole);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("只能设置为教师或学生。");
        }
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("不能从用户管理页授予管理员角色。");
        }
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到这个账户。"));
        account.setRole(role);
    }
}
