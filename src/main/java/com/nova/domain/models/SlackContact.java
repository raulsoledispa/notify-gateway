package com.nova.domain.models;

import java.util.regex.Pattern;

public record SlackContact(String webhookUrl) implements RecipientContact {

    private static final Pattern WEBHOOK_PATTERN = Pattern.compile("^https://hooks\\.slack\\.com/.+$");

    public SlackContact {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must not be null or blank");
        }
        if (!WEBHOOK_PATTERN.matcher(webhookUrl).matches()) {
            throw new IllegalArgumentException("Invalid Slack webhook URL format: " + webhookUrl);
        }
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.SLACK;
    }
}
