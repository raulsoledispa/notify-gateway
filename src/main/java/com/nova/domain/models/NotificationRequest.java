package com.nova.domain.models;

import lombok.Builder;
import java.util.Optional;

@Builder
public record NotificationRequest(
        RecipientContact contact,
        String plainTextBody,
        Template template
) {
    public ChannelType channelType() {
        return contact.channelType();
    }

    public Optional<String> getPlainTextBody() {
        return Optional.ofNullable(plainTextBody);
    }

    public Optional<Template> getTemplate() {
        return Optional.ofNullable(template);
    }
}
