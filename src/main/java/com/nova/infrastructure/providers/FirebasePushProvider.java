package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import com.nova.infrastructure.config.FirebaseConfig;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;
import org.slf4j.LoggerFactory;

public class FirebasePushProvider implements NotificationProvider {

    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(FirebasePushProvider.class));
    private final FirebaseConfig config;

    public FirebasePushProvider(FirebaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("FirebaseConfig cannot be null");
        }
        this.config = config;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[FIREBASE PUSH] Preparing to send push notification securely using service account.");
        // Here we would typically use Firebase Admin SDK using 'this.config.serviceAccountKey()'
        // and targeting project 'this.config.projectId()'
        // For now, per instructions, we do not call the external API.
        log.info("[FIREBASE PUSH] Payload processed successfully for token: {}", request.recipient().getPushToken().orElse("UNKNOWN"));
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.PUSH;
    }
}
