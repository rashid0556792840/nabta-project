package com.nabta.backend.controller;
import com.nabta.backend.dto.AuthDtos.*;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @GetMapping("/me") public UserSummary me(@AuthenticationPrincipal AppUser user) { return auth.summary(user); }
}
