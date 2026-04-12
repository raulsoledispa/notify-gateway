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

---

## API Reference

### Core Components

| Component | Type | Responsibility |
| :--- | :--- | :--- |
| **`NotificationService`** | Interface | The primary orchestrator. Routes requests by inspecting the `RecipientContact` type. Exposes `sendSync` and `sendAsync`. |
| **`NotifyBuilder`** | Class | The configuration builder (`infrastructure/config`) that securely encapsulates the assembly of providers, resilience policies, and template engines. Returns an immutable configuration ready for production injection. |
| **`NotificationRequest`** | Record | The command DTO. Requires a `RecipientContact` and either a `plainTextBody` or a `Template`. The `channelType()` method is derived from the contact — no redundant field. |
| **`RecipientContact`** | Sealed Interface | Java 21 sealed type permitting `EmailContact`, `SmsContact`, and `PushContact`. The contact type IS the channel. Each record owns its own format validation. Adding a new channel type triggers compile-time exhaustiveness errors in all unhandled `switch` expressions. |
| **`ChannelType`** | Enum | Routing discriminator: `EMAIL`, `SMS`, or `PUSH`. Derived automatically from the contact — callers never set it directly. |

---

## Architecture

This system strongly enforces **Hexagonal Architecture**.
- **Domain:** Pure business logic defining what a notification means. Independent of any frameworks or details like databases/vendors.
- **Application:** Organizes the logic and orchestrates interaction between outer and inner layers. Contains services like `NotificationServiceImpl` and structural strategies.
- **Infrastructure:** Framework integration, config parsing, JSON mapping, and external API calls (e.g., `TwilioSmsProvider`, `SendGridEmailProvider`). Depends heavily on Domain ports for isolation.
