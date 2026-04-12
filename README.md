# Notify Gateway

A robust, framework-agnostic Java 21 notification library built around Hexagonal Architecture. Notify Gateway provides a unified, extensible interface for sending notifications across multiple channels (Email, SMS, Push, Slack) while encouraging strict coding standards, immutability, and null-safety.

## Features

- **Multi-Channel Support:** Effortlessly dispatch Email, SMS, Push, and Slack notifications.
- **Bulk Notifications:** Send the same message to multiple recipients of the same channel in a single call, with per-recipient success/failure tracking.
- **Status Events:** Subscribe to notification lifecycle events (PENDING, RETRY, SUCCESS, FAILURE) for logging, metrics, and alerting.
- **Resilience:** Built on Resilience4j for custom retry policies, automatic fault tolerance, and decorators.
- **Provider Agnostic:** Swap providers dynamically (e.g., SendGrid, Twilio, Firebase, Slack) without altering your core application logic.
- **High Performance:** Designed to dynamically leverage Java 21 Virtual Threads for robust, highly concurrent I/O operations.
- **Security & Logging:** Built-in automated PII masking of sensitive information in the application logs.
- **Functional Approach:** Embraces Java `Record` definitions, `Optional` types, and `Result` pattern structures over exception-driven logic.

## Supported Providers

| Channel | Supported Provider | Implementation Class |
| :--- | :--- | :--- |
| **Email** | SendGrid | `SendGridEmailProvider` |
| **SMS** | Twilio | `TwilioSmsProvider` |
| **Push** | Firebase Cloud Messaging | `FirebasePushProvider` |
| **Slack** | Slack Incoming Webhooks | `SlackProvider` |

---

## Quick Start & Full Configuration

To get started quickly, you instantiate the `NotificationService` once globally using `NotifyBuilder`. 

Here is a full, production-ready configuration that activates all notification channels and event listeners:

```java
import com.nova.application.services.NotificationService;
import com.nova.infrastructure.config.*;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;

// 1. Securely load your credentials from environment variables
String sendGridKey     = System.getenv("SENDGRID_API_KEY");
String twilioSid       = System.getenv("TWILIO_ACCOUNT_SID");
String twilioAuth      = System.getenv("TWILIO_AUTH_TOKEN");
String twilioFromPhone = System.getenv("TWILIO_FROM_PHONE");
String firebaseKey     = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
String firebaseProject = System.getenv("FIREBASE_PROJECT_ID");
String slackWebhook    = System.getenv("SLACK_WEBHOOK_URL");

// 2. (Optional) Define a custom Resilience4j retry policy
Retry customRetry = Retry.of("notifyRetry", RetryConfig.custom()
        .maxAttempts(5)
        .waitDuration(Duration.ofSeconds(1))
        .build());

// 3. Instantiate the main service using top-level Config value objects
NotificationService notificationService = NotifyBuilder.builder()
        .withSendGrid(new SendGridConfig(sendGridKey, "no-reply@nova.com", "Nova Support"))
        .withTwilio(new TwilioConfig(twilioSid, twilioAuth, twilioFromPhone))
        .withFirebase(new FirebaseConfig(firebaseKey, firebaseProject))
        .withSlack(new SlackConfig(slackWebhook))
        .withRetryPolicy(customRetry)  // overrides default 3 attempts / 500ms
        .withEventListener(event -> System.out.println("[EVENT] " + event.status() + ": " + event.message()))
        .build();
```

---

## Usage Examples

Once configured, use the `notificationService` dynamically. The gateway routes your payload based on the type of `RecipientContact` you provide — no `ChannelType` field needed.

### 1. Sending an Email

```java
import com.nova.domain.models.EmailContact;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.result.Result;
import com.nova.domain.models.Template;
import java.util.Map;

// (Optional) Use a Template or plain text body
Template emailTemplate = new Template("welcome_email_01", Map.of("userName", "Alice"));

NotificationRequest emailRequest = NotificationRequest.builder()
    .contact(new EmailContact("user@example.com"))
    .template(emailTemplate)
    .build();

Result<Void> result = notificationService.sendSync(emailRequest);

if (result instanceof Result.Success<Void>) {
    System.out.println("Email sent successfully!");
} else if (result instanceof Result.Failure<Void> failure) {
    System.err.println("Email failed: " + failure.message());
}
```

### 2. Sending an SMS

The phone number must adhere strictly to the **E.164** format — `SmsContact` validates this at construction time.

```java
import com.nova.domain.models.SmsContact;

NotificationRequest smsRequest = NotificationRequest.builder()
    .contact(new SmsContact("+12345678901"))
    .plainTextBody("Nova Alert: Your one-time passcode is 987654.")
    .build();

Result<Void> result = notificationService.sendSync(smsRequest);

if (result instanceof Result.Success<Void>) {
    System.out.println("SMS sent successfully!");
}
```

### 3. Sending a Push Notification

```java
import com.nova.domain.models.PushContact;

NotificationRequest pushRequest = NotificationRequest.builder()
    .contact(new PushContact("fcm_device_token"))
    .plainTextBody("Nova: Your order has been shipped and is on its way!")
    .build();

Result<Void> result = notificationService.sendSync(pushRequest);

if (result instanceof Result.Success<Void>) {
    System.out.println("Push Notification delivered successfully!");
}
```

### 4. Sending a Slack Notification

The webhook URL must be a valid Slack incoming webhook starting with `https://hooks.slack.com/`.

```java
import com.nova.domain.models.SlackContact;

NotificationRequest slackRequest = NotificationRequest.builder()
    .contact(new SlackContact("https://hooks.slack.com/services/T00/B00/xxxx"))
    .plainTextBody("Deploy v2.1.0 completed successfully.")
    .build();

Result<Void> result = notificationService.sendSync(slackRequest);

if (result instanceof Result.Success<Void>) {
    System.out.println("Slack message sent!");
}
```

### 5. Sending Bulk Notifications

Send the same message to multiple recipients of the **same channel type** in a single call. Each send is independent — one failure does not stop others.

```java
import com.nova.domain.models.BulkNotificationRequest;
import com.nova.domain.models.BulkNotificationResult;
import java.util.List;

BulkNotificationRequest bulkRequest = BulkNotificationRequest.builder()
    .contacts(List.of(
        new EmailContact("alice@example.com"),
        new EmailContact("bob@example.com"),
        new EmailContact("carol@example.com")
    ))
    .plainTextBody("System maintenance scheduled for tonight at 10 PM.")
    .build();

BulkNotificationResult bulkResult = notificationService.sendBulk(bulkRequest);

System.out.println("Sent: " + bulkResult.successes().size());
System.out.println("Failed: " + bulkResult.failures().size());

bulkResult.failures().forEach(failure ->
    System.err.println("Failed for " + failure.contact() + ": " + failure.errorMessage())
);
```

> **Note:** All contacts in a bulk request must share the same channel type. Mixing email and SMS contacts in the same request will throw an `IllegalArgumentException`.

### 6. Listening to Status Events

Register event listeners to track notification lifecycle events for logging, metrics, or alerting. Events are emitted at two levels:

- **Service layer:** `PENDING`, `SUCCESS`, `FAILURE`
- **Retry decorator:** `RETRY` (emitted on each retry attempt with the attempt number)

```java
import com.nova.domain.ports.NotificationEventListener;

// Register one or more listeners during service configuration
NotificationService service = NotifyBuilder.builder()
    .withSendGrid(new SendGridConfig(apiKey, "no-reply@nova.com", "Nova"))
    .withEventListener(event -> {
        switch (event.status()) {
            case PENDING -> logger.info("Sending to {}", event.request().channelType());
            case RETRY   -> logger.warn("Retrying: {}", event.message());
            case SUCCESS -> metricsCounter.increment("notify.success");
            case FAILURE -> alertService.fire("Notification failed: " + event.message());
        }
    })
    .build();
```

Events are synchronous and best-effort — a failing listener will not block other listeners or the notification itself.

---

## API Reference

### Core Components

| Component | Type | Responsibility |
| :--- | :--- | :--- |
| **`NotificationService`** | Interface | The primary orchestrator. Routes requests by inspecting the `RecipientContact` type. Exposes `sendSync`, `sendAsync`, and `sendBulk`. |
| **`NotifyBuilder`** | Class | The configuration builder (`infrastructure/config`) that securely encapsulates the assembly of providers, resilience policies, template engines, and event listeners. Returns an immutable configuration ready for production injection. |
| **`NotificationRequest`** | Record | The command DTO. Requires a `RecipientContact` and either a `plainTextBody` or a `Template`. The `channelType()` method is derived from the contact — no redundant field. |
| **`BulkNotificationRequest`** | Record | Bulk command DTO. Contains a list of `RecipientContact` (all same channel type) and a shared message body or template. |
| **`BulkNotificationResult`** | Record | Bulk response. Contains `successes` (list of contacts) and `failures` (list of `BulkFailureDetail` with contact and error message). |
| **`RecipientContact`** | Sealed Interface | Java 21 sealed type permitting `EmailContact`, `SmsContact`, `PushContact`, and `SlackContact`. The contact type IS the channel. Each record owns its own format validation. Adding a new channel type triggers compile-time exhaustiveness errors in all unhandled `switch` expressions. |
| **`ChannelType`** | Enum | Routing discriminator: `EMAIL`, `SMS`, `PUSH`, or `SLACK`. Derived automatically from the contact — callers never set it directly. |
| **`NotificationEventListener`** | Functional Interface | Outbound port for receiving lifecycle events. Implement `onEvent(NotificationEvent)` and register via `NotifyBuilder.withEventListener()`. |
| **`NotificationEvent`** | Record | Event payload containing `status` (PENDING, RETRY, SUCCESS, FAILURE), `request`, `message`, and `timestamp`. |

---

## Architecture

This system strongly enforces **Hexagonal Architecture**.
- **Domain:** Pure business logic defining what a notification means. Independent of any frameworks or details like databases/vendors.
- **Application:** Organizes the logic and orchestrates interaction between outer and inner layers. Contains services like `NotificationServiceImpl` and structural strategies.
- **Infrastructure:** Framework integration, config parsing, JSON mapping, and external API calls (e.g., `TwilioSmsProvider`, `SendGridEmailProvider`, `SlackProvider`). Depends heavily on Domain ports for isolation.
