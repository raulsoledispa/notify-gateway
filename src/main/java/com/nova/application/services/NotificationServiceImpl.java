package com.nova.application.services;

import com.nova.application.strategies.NotificationStrategy;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.TemplateEngine;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRequestValidator validator;
    private final TemplateProcessor templateProcessor;
    private final StrategyResolver strategyResolver;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public NotificationServiceImpl(List<NotificationStrategy> strategies, TemplateEngine templateEngine) {
        this.validator = new NotificationRequestValidator();
        this.templateProcessor = new TemplateProcessor(templateEngine);
        this.strategyResolver = new StrategyResolver(strategies);
    }

    @Override
    public Result<Void> sendSync(NotificationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            Result<Void> validation = validator.validate(request);
            if (validation instanceof Result.Failure) {
                return validation;
            }

            Result<NotificationStrategy> strategyResult = strategyResolver.resolve(request);
            if (strategyResult instanceof Result.Failure<NotificationStrategy>(String message, Throwable cause)) {
                return new Result.Failure<>(message, cause);
            }

            NotificationRequest processedRequest = templateProcessor.process(request);

            log.info("Dispatching notification to: {}", request.channelType());
            return ((Result.Success<NotificationStrategy>) strategyResult).value().execute(processedRequest);
        } catch (Exception e) {
            log.error("Unexpected error during processing", e);
            return new Result.Failure<>("Unexpected error: " + e.getMessage(), e);
        } finally {
            log.info("sendSync request processed in {} ms", (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public CompletableFuture<Result<Void>> sendAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> sendSync(request), executorService);
    }
}
