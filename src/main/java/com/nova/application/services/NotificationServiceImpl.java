package com.nova.application.services;

import com.nova.application.strategies.NotificationStrategy;
import com.nova.domain.models.BulkNotificationRequest;
import com.nova.domain.models.BulkNotificationResult;
import com.nova.domain.models.NotificationEvent;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.NotificationStatus;
import com.nova.domain.models.RecipientContact;
import com.nova.domain.ports.TemplateEngine;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRequestValidator validator;
    private final TemplateProcessor templateProcessor;
    private final StrategyResolver strategyResolver;
    private final NotificationEventPublisher eventPublisher;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public NotificationServiceImpl(List<NotificationStrategy> strategies, TemplateEngine templateEngine) {
        this(strategies, templateEngine, NotificationEventPublisher.empty());
    }

    public NotificationServiceImpl(List<NotificationStrategy> strategies, TemplateEngine templateEngine, NotificationEventPublisher eventPublisher) {
        this.validator = new NotificationRequestValidator();
        this.templateProcessor = new TemplateProcessor(templateEngine);
        this.strategyResolver = new StrategyResolver(strategies);
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<Void> sendSync(NotificationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            Result<Void> validation = validator.validate(request);
            if (validation instanceof Result.Failure) {
                publishEvent(NotificationStatus.FAILURE, request, "Validation failed");
                return validation;
            }

            Result<NotificationStrategy> strategyResult = strategyResolver.resolve(request);
            if (strategyResult instanceof Result.Failure<NotificationStrategy>(String message, Throwable cause)) {
                publishEvent(NotificationStatus.FAILURE, request, message);
                return new Result.Failure<>(message, cause);
            }

            publishEvent(NotificationStatus.PENDING, request, "Dispatching notification");

            log.info("Dispatching notification to: {}", request.channelType());
            Result<Void> result = ((Result.Success<NotificationStrategy>) strategyResult).value().execute(request);

            if (result instanceof Result.Success) {
                publishEvent(NotificationStatus.SUCCESS, request, "Notification sent successfully");
            } else if (result instanceof Result.Failure<Void> failure) {
                publishEvent(NotificationStatus.FAILURE, request, failure.message());
            }

            return result;
        } catch (Exception e) {
            log.error("Unexpected error during processing", e);
            publishEvent(NotificationStatus.FAILURE, request, "Unexpected error: " + e.getMessage());
            return new Result.Failure<>("Unexpected error: " + e.getMessage(), e);
        } finally {
            log.info("sendSync request processed in {} ms", (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public CompletableFuture<Result<Void>> sendAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> sendSync(request), executorService);
    }

    @Override
    public BulkNotificationResult sendBulk(BulkNotificationRequest request) {
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Map.Entry<RecipientContact, Result<Void>>>> futures = request.contacts().stream()
                .map(contact -> CompletableFuture.supplyAsync(() -> {
                    NotificationRequest individual = NotificationRequest.builder()
                            .contact(contact)
                            .plainTextBody(request.plainTextBody())
                            .template(request.template())
                            .build();
                    return Map.entry(contact, sendSync(individual));
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<RecipientContact> successes = new ArrayList<>();
        List<BulkNotificationResult.BulkFailureDetail> failures = new ArrayList<>();

        for (var future : futures) {
            var entry = future.join();
            switch (entry.getValue()) {
                case Result.Success<?> s -> successes.add(entry.getKey());
                case Result.Failure<?> f -> failures.add(
                        new BulkNotificationResult.BulkFailureDetail(entry.getKey(), f.message()));
            }
        }

        log.info("sendBulk processed {} requests in {} ms", request.contacts().size(), (System.currentTimeMillis() - startTime));
        return new BulkNotificationResult(successes, failures);
    }

    private void publishEvent(NotificationStatus status, NotificationRequest request, String message) {
        eventPublisher.publish(NotificationEvent.builder()
                .status(status)
                .request(request)
                .message(message)
                .timestamp(Instant.now())
                .build());
    }
}
