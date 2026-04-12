package com.nova.infrastructure.resilience;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;

public class Resilience4jRetryDecorator implements NotificationProvider {

    private final NotificationProvider delegate;
    private final Retry retry;

    public Resilience4jRetryDecorator(NotificationProvider delegate, Retry retry) {
        this.delegate = delegate;
        this.retry = retry;
    }

    @Override
    public Result<Void> send(NotificationRequest request) {
        Supplier<Result<Void>> supplier = () -> {
            Result<Void> currentResult = delegate.send(request);
            if (currentResult instanceof Result.Failure) {
                // We throw an exception to trigger the resilience4j retry mechanism since we are avoiding standard exception throwing in our domain.
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
