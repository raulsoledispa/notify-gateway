package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import com.nova.infrastructure.config.TwilioConfig;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;
import org.slf4j.LoggerFactory;

public class TwilioSmsProvider implements NotificationProvider {

    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(TwilioSmsProvider.class));
    private final TwilioConfig config;

    public TwilioSmsProvider(TwilioConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("TwilioConfig cannot be null");
        }
        this.config = config;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[TWILIO SMS] Preparing to send SMS with Account SID: {}...", config.accountSid().substring(0, Math.min(3, config.accountSid().length())) + "***");
        // Here we would typically use Twilio HTTP client using 'this.config.accountSid()' and 'this.config.authToken()'
        // with fromPhoneNumber 'this.config.fromPhoneNumber()'
        // For now, per instructions, we do not call the external API.
        log.info("[TWILIO SMS] Payload processed successfully for phone: {}", request.recipient().getPhoneNumber().orElse("UNKNOWN"));
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.SMS;
    }
}
