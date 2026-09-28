package io.github.homeworkportal.account;

import io.github.homeworkportal.domain.UserAccount;
import io.github.homeworkportal.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PortalUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public PortalUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = users.findByUsername(username.toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .disabled(!account.isEnabled())
                .build();
    }
}
