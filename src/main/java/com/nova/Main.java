package com.nova;

import com.nova.application.services.NotificationService;
import com.nova.domain.models.EmailContact;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.NotificationStatus;
import com.nova.domain.result.Result;
import com.nova.infrastructure.config.NotifyBuilder;
import com.nova.infrastructure.config.SendGridConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Notify Gateway - Starting library verification");

        String sendGridApiKey = System.getenv("SENDGRID_API_KEY");

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY not set. Running in demo mode with a placeholder key.");
            sendGridApiKey = "demo-key";
        }

        NotificationService service = NotifyBuilder.builder()
                .withSendGrid(new SendGridConfig(sendGridApiKey, "noreply@nova.com", "Nova Notifications"))
                .withEventListener(event -> log.info("Event: status={}, message={}", event.status(), event.message()))
                .build();

        log.info("NotificationService built successfully with SendGrid provider");

        NotificationRequest request = NotificationRequest.builder()
                .contact(new EmailContact("user@example.com"))
                .plainTextBody("Hello from Notify Gateway!")
                .build();

        log.info("Sending test notification...");
        Result<Void> result = service.sendSync(request);

        switch (result) {
            case Result.Success<Void> s -> log.info("Notification sent successfully");
            case Result.Failure<Void> f -> log.info("Notification failed (expected in demo mode): {}", f.message());
        }

        log.info("Notify Gateway - Library verification complete");
    }
}
