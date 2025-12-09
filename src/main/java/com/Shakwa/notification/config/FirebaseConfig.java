package com.Shakwa.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Configuration
 * 
 * This class initializes Firebase Admin SDK for sending push notifications.
 * 
 * Configuration:
 * 1. Place your Firebase service account JSON file in src/main/resources/
 * 2. Set firebase.service-account.path in application.properties
 * 3. Or set FIREBASE_SERVICE_ACCOUNT environment variable
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account.path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${firebase.service-account.env:FIREBASE_SERVICE_ACCOUNT}")
    private String serviceAccountEnv;

    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = buildFirebaseOptions();
                firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } else {
                firebaseApp = FirebaseApp.getInstance();
                log.info("Firebase already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    private FirebaseOptions buildFirebaseOptions() throws IOException {
        InputStream serviceAccount = getServiceAccountInputStream();
        
        if (serviceAccount == null) {
            throw new IOException("Firebase service account file not found. " +
                    "Please provide firebase-service-account.json in resources or set FIREBASE_SERVICE_ACCOUNT environment variable.");
        }

        return FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
    }

    private InputStream getServiceAccountInputStream() throws IOException {
        // Try environment variable first (for production/containerized deployments)
        String envPath = System.getenv(serviceAccountEnv);
        if (envPath != null && !envPath.isEmpty()) {
            try {
                return new FileInputStream(envPath);
            } catch (IOException e) {
                log.warn("Failed to read Firebase service account from environment variable: {}", e.getMessage());
            }
        }

        // Try classpath resource (for development)
        try {
            ClassPathResource resource = new ClassPathResource(serviceAccountPath);
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.warn("Failed to read Firebase service account from classpath: {}", e.getMessage());
        }

        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (firebaseApp == null) {
            throw new IllegalStateException("FirebaseApp is not initialized");
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
