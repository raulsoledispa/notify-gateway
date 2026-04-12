package com.nova.domain.models;

import lombok.Builder;
import java.util.Optional;

@Builder
public record NotificationRequest(
        Recipient recipient,
        ChannelType channelType,
        String plainTextBody,
        Template template
) {
    public Optional<String> getPlainTextBody() {
        return Optional.ofNullable(plainTextBody);
    }

    public Optional<Template> getTemplate() {
        return Optional.ofNullable(template);
    }
}
