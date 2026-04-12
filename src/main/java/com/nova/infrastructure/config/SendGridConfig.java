package com.nova.infrastructure.config;

/**
 * Configuration value object for the SendGrid email provider.
 *
 * @param apiKey    The SendGrid API key used for authentication. Mandatory.
 * @param fromEmail The sender email address shown to recipients. Mandatory.
 * @param fromName  The sender display name shown to recipients. Optional.
 */
public record SendGridConfig(String apiKey, String fromEmail, String fromName) {
    public SendGridConfig {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("SendGrid API key cannot be null or blank");
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalArgumentException("SendGrid fromEmail cannot be null or blank");
        }
    }
}
