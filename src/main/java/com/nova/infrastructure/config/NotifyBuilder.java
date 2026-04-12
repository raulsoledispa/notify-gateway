package com.nova.infrastructure.config;

import com.nova.application.services.NotificationEventPublisher;
import com.nova.application.services.NotificationService;
import com.nova.application.services.NotificationServiceImpl;
import com.nova.application.strategies.EmailStrategy;
import com.nova.application.strategies.NotificationStrategy;
import com.nova.application.strategies.PushStrategy;
import com.nova.application.strategies.SlackStrategy;
import com.nova.application.strategies.SmsStrategy;
import com.nova.domain.ports.NotificationEventListener;
import com.nova.domain.ports.NotificationProvider;
import com.nova.infrastructure.providers.FirebasePushProvider;
import com.nova.infrastructure.providers.SendGridEmailProvider;
import com.nova.infrastructure.providers.SlackProvider;
import com.nova.infrastructure.providers.TwilioSmsProvider;
import com.nova.infrastructure.resilience.Resilience4jRetryDecorator;
import com.nova.infrastructure.templates.InMemoryTemplateEngine;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotifyBuilder {
    private final List<NotificationProvider> providers = new ArrayList<>();
    private final List<NotificationEventListener> eventListeners = new ArrayList<>();
    private Retry retryConfig;
    private com.nova.domain.ports.TemplateEngine templateEngine;

    private NotifyBuilder() {
        this.templateEngine = new InMemoryTemplateEngine();
        this.retryConfig = Retry.of("defaultRetry", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build());
    }

    public static NotifyBuilder builder() {
        return new NotifyBuilder();
    }

    /**
     * Set a custom Resilience4j retry policy instead of the default 3 times, 500ms delay.
     */
    public NotifyBuilder withRetryPolicy(Retry retry) {
        this.retryConfig = retry;
        return this;
    }

    /**
     * Manually add a specific configuration.
     */
    public NotifyBuilder addProvider(NotificationProvider provider) {
        this.providers.add(provider);
        return this;
    }

    public NotifyBuilder withSendGrid(SendGridConfig config) {
        return this.addProvider(new SendGridEmailProvider(config));
    }

    public NotifyBuilder withTwilio(TwilioConfig config) {
        return this.addProvider(new TwilioSmsProvider(config));
    }

    public NotifyBuilder withFirebase(FirebaseConfig config) {
        return this.addProvider(new FirebasePushProvider(config));
    }

    public NotifyBuilder withSlack(SlackConfig config) {
        return this.addProvider(new SlackProvider(config));
    }

    public NotifyBuilder withTemplateEngine(com.nova.domain.ports.TemplateEngine engine) {
        this.templateEngine = engine;
        return this;
    }

    public NotifyBuilder withEventListener(NotificationEventListener listener) {
        this.eventListeners.add(listener);
        return this;
    }

    public NotificationService build() {
        NotificationEventPublisher eventPublisher = eventListeners.isEmpty()
                ? NotificationEventPublisher.empty()
                : new NotificationEventPublisher(eventListeners);

        List<NotificationProvider> decoratedProviders = providers.stream()
                .map(p -> (NotificationProvider) new Resilience4jRetryDecorator(p, retryConfig, eventPublisher))
                .toList();

        NotificationStrategy emailStrategy = new EmailStrategy(decoratedProviders);
        NotificationStrategy smsStrategy = new SmsStrategy(decoratedProviders);
        NotificationStrategy pushStrategy = new PushStrategy(decoratedProviders);
        NotificationStrategy slackStrategy = new SlackStrategy(decoratedProviders);

        List<NotificationStrategy> strategies = Arrays.asList(emailStrategy, smsStrategy, pushStrategy, slackStrategy);
        return new NotificationServiceImpl(strategies, templateEngine, eventPublisher);
    }
}
