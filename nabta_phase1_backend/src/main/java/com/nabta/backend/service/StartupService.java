package com.nabta.backend.service;
import com.nabta.backend.model.*;
import com.nabta.backend.repository.CompanySettingsRepository;
import com.nabta.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class StartupService implements ApplicationRunner {
    private final UserRepository users; private final PasswordEncoder encoder; private final CompanySettingsRepository settings;
    @Value("${nabta.admin.name:}") private String adminName; @Value("${nabta.admin.email:}") private String adminEmail; @Value("${nabta.admin.password:}") private String adminPassword;
    public StartupService(UserRepository users, PasswordEncoder encoder, CompanySettingsRepository settings) { this.users=users; this.encoder=encoder; this.settings=settings; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (!settings.existsById(1L)) settings.save(new CompanySettings());
        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank() && !users.existsByEmailIgnoreCase(adminEmail)) {
            if (adminPassword.length() < 12) throw new IllegalStateException("NABTA_ADMIN_PASSWORD must be at least 12 characters.");
            AppUser user=new AppUser(); user.setName(adminName == null || adminName.isBlank() ? "Nabta Owner" : adminName.trim()); user.setEmail(adminEmail.trim().toLowerCase()); user.setPasswordHash(encoder.encode(adminPassword)); user.setRole(Role.ADMIN); user.setStatus(AccountStatus.ACTIVE); users.save(user);
        }
    }
}
