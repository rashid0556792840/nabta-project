package com.nabta.backend.controller;
import com.nabta.backend.dto.AuthDtos.PasswordChangeRequest;
import com.nabta.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.nabta.backend.dto.AuthDtos.UserSummary;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.repository.UserRepository;
import com.nabta.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
@RestController @RequestMapping("/api/users/me")
public class ProfileController {
    private final UserRepository users; private final AuthService auth;
    public ProfileController(UserRepository users,AuthService auth) { this.users=users; this.auth=auth; }
    @GetMapping public UserSummary get(@AuthenticationPrincipal AppUser user) { return auth.summary(user); }
    @PutMapping public UserSummary update(@AuthenticationPrincipal AppUser user,@Valid @RequestBody ProfileUpdateRequest request,HttpServletResponse response) {
        if (request.name()!=null) {
            String name=request.name().trim();
            if (name.isEmpty()) throw ApiException.badRequest("Name cannot be empty.");
            user.setName(name);
        }
        boolean emailChanged=false;
        if (request.email()!=null) {
            String email=request.email().trim().toLowerCase(java.util.Locale.ROOT);
            if (email.isEmpty()) throw ApiException.badRequest("Email cannot be empty.");
            if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
            emailChanged=!email.equalsIgnoreCase(user.getEmail());
            user.setEmail(email);
        }
        if (request.phone()!=null) user.setPhone(request.phone().isBlank()?null:request.phone().trim());
        if (request.address()!=null) user.setAddress(request.address().isBlank()?null:request.address().trim());
        AppUser saved=users.save(user);
        if (emailChanged) response.setHeader("Authorization", "Bearer " + auth.refreshedToken(saved));
        return auth.summary(saved);
    }
    @PutMapping("/password") public void changePassword(@AuthenticationPrincipal AppUser user,@Valid @RequestBody PasswordChangeRequest request) { auth.changePassword(user,request); }
}
