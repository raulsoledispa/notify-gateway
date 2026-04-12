package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;

public class SendGridEmailProvider implements NotificationProvider {
    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(SendGridEmailProvider.class));
    private final String apiKey;

    public SendGridEmailProvider(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("SendGrid API key cannot be null or blank");
        }
        this.apiKey = apiKey;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[SENDGRID EMAIL] Preparing to send email with API Key: {}...", apiKey.substring(0, Math.min(3, apiKey.length())) + "***");
        // Here we would typically use SendGrid HTTP client using 'this.apiKey'
        log.info("[SENDGRID EMAIL] Sending Content: [{}]", request.getPlainTextBody().orElse("No Body Provided"));
        // For now, per instructions, we do not call the external API.
        log.info("[SENDGRID EMAIL] Payload processed successfully for recipient: {}", request.recipient().getEmail().orElse("UNKNOWN"));
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.EMAIL;
    }
}
