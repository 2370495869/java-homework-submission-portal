package io.github.homeworkportal.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "用户名需为 3–50 位字母、数字、点、下划线或短横线")
    private String username;

    @NotBlank
    @Size(max = 80, message = "显示名称最多 80 个字符")
    private String displayName;

    @NotBlank
    @Size(min = 12, max = 64, message = "密码长度需为 12–64 个字符")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
