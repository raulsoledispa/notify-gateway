package com.nova.infrastructure.resilience;

import com.nova.application.services.NotificationEventPublisher;
import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationEvent;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.NotificationStatus;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import io.github.resilience4j.retry.Retry;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ResilienceRetryDecorator implements NotificationProvider {

    private final NotificationProvider delegate;
    private final Retry retry;
    private final NotificationEventPublisher eventPublisher;

    public ResilienceRetryDecorator(NotificationProvider delegate, Retry retry) {
        this(delegate, retry, null);
    }

    public ResilienceRetryDecorator(NotificationProvider delegate, Retry retry, NotificationEventPublisher eventPublisher) {
        this.delegate = delegate;
        this.retry = retry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        publishEvent(request, NotificationStatus.PENDING, "Notification pending");

        try {
            Result<Void> result = Retry.decorateSupplier(retry, buildSupplier(request)).get();
            publishEvent(request, NotificationStatus.SUCCESS, "Notification sent successfully");
            return result;
        } catch (RetryableException e) {
            publishEvent(request, NotificationStatus.FAILURE, "Failed after retries: " + e.getMessage());
            return new Result.Failure<>("Failed after retries: " + e.getMessage(), e);
        } catch (Exception e) {
            publishEvent(request, NotificationStatus.FAILURE, "Unexpected failure during send: " + e.getMessage());
            return new Result.Failure<>("Unexpected failure during send: " + e.getMessage(), e);
        }
    }

    private Supplier<Result<Void>> buildSupplier(NotificationRequest request) {
        AtomicInteger attempt = new AtomicInteger(0);
        return () -> {
            int currentAttempt = attempt.incrementAndGet();
            if (currentAttempt > 1) {
                publishEvent(request, NotificationStatus.RETRY, "Retry attempt " + currentAttempt);
            }
            Result<Void> result = delegate.send(request);
            if (result instanceof Result.Failure) {
                throw new RetryableException(((Result.Failure<Void>) result).message());
            }
            return result;
        };
    }

    private void publishEvent(NotificationRequest request, NotificationStatus status, String message) {
        if (eventPublisher != null) {
            eventPublisher.publish(NotificationEvent.builder()
                    .status(status)
                    .request(request)
                    .message(message)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    @Override
    public ChannelType getSupportedChannel() {
        return delegate.getSupportedChannel();
    }

    private static class RetryableException extends RuntimeException {
        RetryableException(String message) {
            super(message);
        }
    }
}
