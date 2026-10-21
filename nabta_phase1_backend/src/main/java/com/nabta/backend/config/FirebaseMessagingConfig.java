package com.nabta.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableAsync
public class FirebaseMessagingConfig {
    @Bean
    @ConditionalOnProperty(name = "nabta.fcm.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(@Value("${nabta.fcm.service-account-path:}") String serviceAccountPath) throws IOException {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            throw new IllegalStateException("FCM is enabled, but NABTA_FCM_SERVICE_ACCOUNT_PATH is empty.");
        }
        for (FirebaseApp existing : FirebaseApp.getApps()) {
            if ("nabta-fcm".equals(existing.getName())) return FirebaseMessaging.getInstance(existing);
        }
        Path keyPath = Path.of(serviceAccountPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(keyPath)) {
            throw new IllegalStateException("Firebase service-account file was not found at the configured path.");
        }
        try (InputStream input = Files.newInputStream(keyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(input))
                .build();
            FirebaseApp app = FirebaseApp.initializeApp(options, "nabta-fcm");
            return FirebaseMessaging.getInstance(app);
        }
    }
}
