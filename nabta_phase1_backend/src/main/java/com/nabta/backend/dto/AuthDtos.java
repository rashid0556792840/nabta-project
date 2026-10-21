package com.nabta.backend.dto;
import com.nabta.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=190) String email, @NotBlank @Size(min=8, max=72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserSummary(Long id, String name, String email, Role role, LocalDateTime createdAt, String phone, String address) {}
    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserSummary user) {}
    public record ProfileUpdateRequest(@Size(max=100) String name, @Email @Size(max=190) String email,
                                       @Pattern(regexp="^$|^[0-9]{7,20}$", message="Phone must contain at least 7 digits and digits only.") @Size(max=20) String phone,
                                       @Size(max=1500) String address) {}
    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @Size(min=8, max=72) String newPassword) {}
}
