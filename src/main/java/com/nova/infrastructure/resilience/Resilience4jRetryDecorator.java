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

public class Resilience4jRetryDecorator implements NotificationProvider {

    private final NotificationProvider delegate;
    private final Retry retry;
    private final NotificationEventPublisher eventPublisher;

    public Resilience4jRetryDecorator(NotificationProvider delegate, Retry retry) {
        this(delegate, retry, null);
    }

    public Resilience4jRetryDecorator(NotificationProvider delegate, Retry retry, NotificationEventPublisher eventPublisher) {
        this.delegate = delegate;
        this.retry = retry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        AtomicInteger attempt = new AtomicInteger(0);

        Supplier<Result<Void>> supplier = () -> {
            int currentAttempt = attempt.incrementAndGet();
            if (currentAttempt > 1 && eventPublisher != null) {
                eventPublisher.publish(NotificationEvent.builder()
                        .status(NotificationStatus.RETRY)
                        .request(request)
                        .message("Retry attempt " + currentAttempt)
                        .timestamp(Instant.now())
                        .build());
            }

            Result<Void> currentResult = delegate.send(request);
            if (currentResult instanceof Result.Failure) {
                throw new RetryableException(((Result.Failure<Void>) currentResult).message());
            }
            return currentResult;
        };

        try {
            return Retry.decorateSupplier(retry, supplier).get();
        } catch (RetryableException e) {
            return new Result.Failure<>("Failed after retries: " + e.getMessage(), e);
        } catch (Exception e) {
            return new Result.Failure<>("Unexpected failure during send: " + e.getMessage(), e);
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
