package com.nova.application.services;

import com.nova.application.strategies.NotificationStrategy;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final Map<com.nova.domain.models.ChannelType, NotificationStrategy> strategyMap;
    private final com.nova.domain.ports.TemplateEngine templateEngine;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public NotificationServiceImpl(List<NotificationStrategy> strategies, com.nova.domain.ports.TemplateEngine templateEngine) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotificationStrategy::getChannelType, s -> s));
        this.templateEngine = templateEngine;
    }

    @Override
    public Result<Void> sendSync(NotificationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            if (request == null || request.channelType() == null) {
                return new Result.Failure<>("Invalid request or channel missing");
            }
            
            NotificationStrategy strategy = strategyMap.get(request.channelType());
            if (strategy == null) {
                return new Result.Failure<>("No strategy found for channel: " + request.channelType());
            }

            NotificationRequest processedRequest = request;
            if (request.getTemplate().isPresent() && templateEngine != null) {
                String resolvedBody = templateEngine.resolve(request.template());
                processedRequest = NotificationRequest.builder()
                        .recipient(request.recipient())
                        .channelType(request.channelType())
                        .plainTextBody(resolvedBody)
                        .template(request.template())
                        .build();
                log.info("Template successfully resolved into formatting plaintext body.");
            }

            log.info("Dispatching notification asynchronously to: {}", request.channelType());
            Result<Void> result = strategy.execute(processedRequest);
            
            if (result instanceof Result.Success) {
                log.info("Notification successfully sent to {}", request.channelType());
            } else if (result instanceof Result.Failure<Void> failure) {
                log.error("Failed to send notification. Error: {}", failure.message(), failure.cause());
            }
            
            return result;
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
