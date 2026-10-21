package com.nabta.backend.security;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService=jwtService; this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null) {
            String subject=jwtService.subject(header.substring(7));
            java.util.Optional<AppUser> principal=java.util.Optional.empty();
            if (subject != null) {
                try { principal=users.findById(Long.parseLong(subject)); }
                catch (NumberFormatException legacyToken) { principal=users.findByEmailIgnoreCase(subject); }
            }
            principal.filter(AppUser::isEnabled).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(request.getRemoteAddr()); SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request,response);
    }
}
