package com.nova.domain.models;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationEvent(
        NotificationStatus status,
        NotificationRequest request,
        String message,
        Instant timestamp
) {}
