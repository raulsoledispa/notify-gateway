package com.nova.infrastructure.config;

/**
 * Configuration value object for the Firebase Cloud Messaging provider.
 *
 * @param serviceAccountKey The JSON content or file path of the Firebase service account key. Mandatory.
 * @param projectId         The Firebase project ID used to identify the target application environment.
 */
public record FirebaseConfig(String serviceAccountKey, String projectId) {
    public FirebaseConfig {
        if (serviceAccountKey == null || serviceAccountKey.isBlank()) {
            throw new IllegalArgumentException("Firebase service account key cannot be empty");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Firebase project ID cannot be empty");
        }
    }
}
