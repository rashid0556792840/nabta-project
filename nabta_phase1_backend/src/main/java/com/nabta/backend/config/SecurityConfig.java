package com.nabta.backend.config;
import com.nabta.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health", "/api/company", "/api/media/**", "/api/payments/readiness").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/academic/jsp/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/WEB-INF/jsp/products.jsp").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/supervisor/**").hasRole("SUPERVISOR")
                .requestMatchers("/api/delivery/**").hasRole("DELIVERY")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required.\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to perform this action.\"}"); }))
            .build();
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${nabta.cors.allowed-origins:http://localhost:8100,http://localhost:5173,http://localhost:8080,http://localhost,https://localhost,capacitor://localhost,ionic://localhost}") String origins) {
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key")); c.setExposedHeaders(List.of("Location", "Authorization")); c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",c); return source;
    }
}
