package com.nabta.backend.service;
import com.nabta.backend.dto.AuthDtos.*;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.*;
import com.nabta.backend.repository.UserRepository;
import com.nabta.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.encoder=encoder; this.jwt=jwt; }
    @Transactional public AuthResponse register(RegisterRequest request) {
        String email=request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        AppUser user=new AppUser(); user.setName(request.name().trim()); user.setEmail(email); user.setPasswordHash(encoder.encode(request.password())); user.setRole(Role.CUSTOMER); user.setStatus(AccountStatus.ACTIVE);
        return response(users.save(user));
    }
    public AuthResponse login(LoginRequest request) {
        AppUser user=users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(request.password(),user.getPasswordHash()) || !user.isEnabled()) throw new BadCredentialsException("Invalid credentials");
        return response(user);
    }
    public String refreshedToken(AppUser user) { return jwt.generate(user); }
    public UserSummary summary(AppUser user) { return new UserSummary(user.getId(),user.getName(),user.getEmail(),user.getRole(),user.getCreatedAt(),user.getPhone(),user.getAddress()); }
    @Transactional public void changePassword(AppUser user, PasswordChangeRequest request) {
        AppUser current=users.findById(user.getId()).orElseThrow(() -> ApiException.notFound("User not found."));
        if (!encoder.matches(request.currentPassword(),current.getPasswordHash())) throw ApiException.badRequest("Current password is incorrect.");
        if (encoder.matches(request.newPassword(),current.getPasswordHash())) throw ApiException.badRequest("Choose a password different from the current password.");
        current.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(current);
    }
    private AuthResponse response(AppUser user) { return new AuthResponse(jwt.generate(user),"Bearer",jwt.getExpirationSeconds(),summary(user)); }
}
