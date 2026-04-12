# Notify Gateway

A robust, framework-agnostic Java 21 notification library built around Hexagonal Architecture. Notify Gateway provides a unified, extensible interface for sending notifications across multiple channels (Email, SMS, Push) while encouraging strict coding standards, immutability, and null-safety.

## Features

- **Multi-Channel Support:** Effortlessly dispatch Email, SMS, and Push notifications.
- **Resilience:** Built on Resilience4j for custom retry policies, automatic fault tolerance, and decorators.
- **Provider Agnostic:** Swap providers dynamically (e.g., SendGrid, Twilio, Firebase) without altering your core application logic.
- **High Performance:** Designed to dynamically leverage Java 21 Virtual Threads for robust, highly concurrent I/O operations.
- **Security & Logging:** Built-in automated PII masking of sensitive information in the application logs.
- **Functional Approach:** Embraces Java `Record` definitions, `Optional` types, and `Result` pattern structures over exception-driven logic.

## Supported Providers

| Channel | Supported Provider | Implementation Class |
| :--- | :--- | :--- |
| **Email** | SendGrid | `SendGridEmailProvider` |
| **SMS** | Twilio | `TwilioSmsProvider` |
| **Push** | Firebase Cloud Messaging | `FirebasePushProvider` |

---

## Quick Start & Full Configuration

To get started quickly, you instantiate the `NotificationService` once globally using `NotifyBuilder`. 

Here is a full, production-ready configuration that activates all notification channels instantly by retrieving keys from the environment:

```java
import com.nova.application.services.NotificationService;
import com.nova.infrastructure.config.FirebaseConfig;
import com.nova.infrastructure.config.NotifyBuilder;
import com.nova.infrastructure.config.SendGridConfig;
import com.nova.infrastructure.config.TwilioConfig;
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
        .withRetryPolicy(customRetry)  // overrides default 3 attempts / 500ms
        .build();
```

---

## Usage Examples

Once configured, use the `notificationService` dynamically. The gateway intelligently routes your payload based on the designated `ChannelType`.

### 1. Sending an Email

```java
import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.Recipient;
import com.nova.domain.models.Result;
import com.nova.domain.models.Template;
import java.util.Map;

// Configure the recipient details
Recipient recipient = new Recipient(
    "user@example.com", // email
    null,               // phone
    null                // push token
);

// (Optional) Use a Template or plain text body
Template emailTemplate = new Template("welcome_email_01", Map.of("userName", "Alice"));

NotificationRequest emailRequest = NotificationRequest.builder()
    .recipient(recipient)
    .channelType(ChannelType.EMAIL)
    .template(emailTemplate)
    .build();

Result<Void> result = notificationService.send(emailRequest);

if (result.isSuccess()) {
    System.out.println("Email sent successfully!");
} else {
    System.err.println("Email failed: " + result.getError());
}
```

### 2. Sending an SMS

Make sure your `Recipient`'s phone number adheres strictly to the **E.164** format.

```java
Recipient recipient = new Recipient(
    null,               // email
    "+12345678901",     // phone
    null                // push token
);

NotificationRequest smsRequest = NotificationRequest.builder()
    .recipient(recipient)
    .channelType(ChannelType.SMS)
    .plainTextBody("Nova Alert: Your one-time passcode is 987654.")
    .build();

Result<Void> result = notificationService.send(smsRequest);

if (result.isSuccess()) {
    System.out.println("SMS sent successfully!");
}
```

### 3. Sending a Push Notification

```java
Recipient recipient = new Recipient(
    null,               // email
    null,               // phone
    "fcm_device_token"  // push token
);

NotificationRequest pushRequest = NotificationRequest.builder()
    .recipient(recipient)
    .channelType(ChannelType.PUSH)
    .plainTextBody("Nova: Your order has been shipped and is on its way!")
    .build();

Result<Void> result = notificationService.send(pushRequest);

if (result.isSuccess()) {
    System.out.println("Push Notification delivered successfully!");
}
```

---

## API Reference

### Core Components

| Component | Type | Responsibility |
| :--- | :--- | :--- |
| **`NotificationService`** | Interface | The primary orchestrator. Invokes internal domain strategies based on `ChannelType`. Exposes the main method: `Result<Void> send(NotificationRequest request)`. |
| **`NotifyBuilder`** | Class | The configuration builder (`infrastructure/config`) that securely encapsulates the assembly of providers, resilience policies, and template engines. Returns an immutable configuration ready for production injection. |
| **`NotificationRequest`** | Record | The overarching Command DTO. Requires a `Recipient`, `ChannelType`, and either a `plainTextBody` or a `Template`. |
| **`ChannelType`** | Enum | Represents routing values: `EMAIL`, `SMS`, or `PUSH`. |
| **`Recipient`** | Record | Domain model capturing destination endpoints (`email`, `phoneNumber`, `pushToken`). Performs native internal regex validation upon instantiation to safeguard integrity. |

---

## Architecture

This system strongly enforces **Hexagonal Architecture**.
- **Domain:** Pure business logic defining what a notification means. Independent of any frameworks or details like databases/vendors.
- **Application:** Organizes the logic and orchestrates interaction between outer and inner layers. Contains services like `NotificationServiceImpl` and structural strategies.
- **Infrastructure:** Framework integration, config parsing, JSON mapping, and external API calls (e.g., `TwilioSmsProvider`, `SendGridEmailProvider`). Depends heavily on Domain ports for isolation.
