package com.nova.infrastructure.providers;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.SlackContact;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import com.nova.infrastructure.config.SlackConfig;
import com.nova.infrastructure.logging.PrivacyMaskingLogger;
import org.slf4j.LoggerFactory;

public class SlackProvider implements NotificationProvider {

    private static final PrivacyMaskingLogger log = new PrivacyMaskingLogger(LoggerFactory.getLogger(SlackProvider.class));
    private final SlackConfig config;

    public SlackProvider(SlackConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SlackConfig cannot be null");
        }
        this.config = config;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        log.info("[SLACK] Preparing to send message via webhook: {}...", config.webhookUrl().substring(0, Math.min(30, config.webhookUrl().length())) + "***");
        log.info("[SLACK] Sending Content: [{}]", request.getPlainTextBody().orElse("No Body Provided"));
        if (!(request.contact() instanceof SlackContact(String webhookUrl))) {
            return new Result.Failure<>("Expected SlackContact, received: " + request.contact().getClass().getSimpleName());
        }
        log.info("[SLACK] Payload processed successfully for webhook: {}", webhookUrl);
        return new Result.Success<>(null);
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.SLACK;
    }
}
