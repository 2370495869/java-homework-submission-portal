package io.github.homeworkportal.config;

import io.github.homeworkportal.domain.Role;
import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public BootstrapAdminRunner(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.display-name:Administrator}") String displayName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (!hasUsername && !hasPassword) {
            return;
        }
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("Bootstrap admin username and password must be configured together.");
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (!normalizedUsername.matches("[a-z0-9._-]{3,50}")) {
            throw new IllegalStateException("Bootstrap admin username must be 3-50 lowercase letters, digits, dots, underscores, or hyphens.");
        }
        if (users.existsByUsername(normalizedUsername)) {
            UserAccount existing = users.findByUsername(normalizedUsername).orElseThrow();
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalStateException("The configured bootstrap admin name already belongs to a non-admin account.");
            }
            return;
        }
        validateBootstrapPassword(password);
        users.save(new UserAccount(normalizedUsername, safeDisplayName(displayName),
                passwordEncoder.encode(password), Role.ADMIN));
    }

    private static void validateBootstrapPassword(String value) {
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (value.length() < 12 || bytes > 72) {
            throw new IllegalStateException("Bootstrap admin password must be at least 12 characters and at most 72 UTF-8 bytes.");
        }
    }

    private static String safeDisplayName(String value) {
        String result = value == null ? "" : value.trim();
        return result.isBlank() ? "Administrator" : result.substring(0, Math.min(result.length(), 80));
    }
}
