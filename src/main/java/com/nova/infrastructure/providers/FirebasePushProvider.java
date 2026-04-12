package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;

public class FirebasePushProvider implements NotificationProvider {
    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(FirebasePushProvider.class));
    private final String serviceAccountKey;

    public FirebasePushProvider(String serviceAccountKey) {
        if (serviceAccountKey == null || serviceAccountKey.isBlank()) {
            throw new IllegalArgumentException("Firebase service account key cannot be empty");
        }
        this.serviceAccountKey = serviceAccountKey;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[FIREBASE PUSH] Preparing to send push notification securely using service account.");
        // Here we would typically use Firebase Admin SDK using 'this.serviceAccountKey'
        // For now, per instructions, we do not call the external API.
        log.info("[FIREBASE PUSH] Payload processed successfully for token: {}", request.recipient().getPushToken().orElse("UNKNOWN"));
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.PUSH;
    }
}
