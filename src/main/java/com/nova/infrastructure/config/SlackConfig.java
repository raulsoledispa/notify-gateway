package com.nova.infrastructure.config;

/**
 * Configuration value object for the Slack webhook provider.
 *
 * @param webhookUrl The Slack incoming webhook URL. Mandatory.
 */
public record SlackConfig(String webhookUrl) {
    public SlackConfig {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Slack webhook URL cannot be null or blank");
        }
    }
}
