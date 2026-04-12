package com.nova.domain.models;

import lombok.Builder;

import java.util.List;
import java.util.Optional;

@Builder
public record BulkNotificationRequest(
        List<RecipientContact> contacts,
        String plainTextBody,
        Template template
) {
    public BulkNotificationRequest {
        if (contacts == null || contacts.isEmpty()) {
            throw new IllegalArgumentException("Contacts list must not be null or empty");
        }
        ChannelType expected = contacts.getFirst().channelType();
        boolean mixedTypes = contacts.stream()
                .anyMatch(c -> c.channelType() != expected);
        if (mixedTypes) {
            throw new IllegalArgumentException("All contacts must share the same channel type, expected: " + expected);
        }
    }

    public ChannelType channelType() {
        return contacts.getFirst().channelType();
    }

    public Optional<String> getPlainTextBody() {
        return Optional.ofNullable(plainTextBody);
    }

    public Optional<Template> getTemplate() {
        return Optional.ofNullable(template);
    }
}
