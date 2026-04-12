package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.EmailContact;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import com.nova.infrastructure.config.SendGridConfig;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;
import org.slf4j.LoggerFactory;

public class SendGridEmailProvider implements NotificationProvider {

    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(SendGridEmailProvider.class));
    private final SendGridConfig config;

    public SendGridEmailProvider(SendGridConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SendGridConfig cannot be null");
        }
        this.config = config;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[SENDGRID EMAIL] Preparing to send email with API Key: {}...", config.apiKey().substring(0, Math.min(3, config.apiKey().length())) + "***");
        // Here we would typically use SendGrid HTTP client using 'this.config.apiKey()'
        // with sender 'this.config.fromEmail()' / 'this.config.fromName()'
        log.info("[SENDGRID EMAIL] Sending Content: [{}]", request.getPlainTextBody().orElse("No Body Provided"));
        // For now, per instructions, we do not call the external API.
        if (!(request.contact() instanceof EmailContact(String email))) {
            return new Result.Failure<>("Expected EmailContact, received: " + request.contact().getClass().getSimpleName());
        }
        log.info("[SENDGRID EMAIL] Payload processed successfully for recipient: {}", email);
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.EMAIL;
    }
}
