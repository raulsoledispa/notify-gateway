package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;

public class TwilioSmsProvider implements NotificationProvider {
    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(TwilioSmsProvider.class));
    private final String accountSid;
    private final String authToken;

    public TwilioSmsProvider(String accountSid, String authToken) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Twilio credentials cannot be empty");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[TWILIO SMS] Preparing to send SMS with Account SID: {}...", accountSid.substring(0, Math.min(3, accountSid.length())) + "***");
        // Here we would typically use Twilio HTTP client using 'this.accountSid' and 'this.authToken'
        // For now, per instructions, we do not call the external API.
        log.info("[TWILIO SMS] Payload processed successfully for phone: {}", request.recipient().getPhoneNumber().orElse("UNKNOWN"));
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.SMS;
    }
}
