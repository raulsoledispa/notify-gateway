# Building the Framework-Agnostic Notification Gateway Library

This implementation plan details the creation of a framework-agnostic Java 21 notification library that supports sending Emails, SMS, and Push notifications synchronously and asynchronously. The library incorporates Hexagonal Architecture, FP methodologies, and specific design patterns (Result, Strategy, Builder) as defined by the project's coding standards.

## User Review Required

> [!IMPORTANT]  
> The library avoids DI framework annotations (like `@Component` or `@Inject`) to remain truly agnostic. Dependency Injection will be achieved via manual wiring through builder patterns at the library's root entry point (`NotifyBuilder`).

> [!TIP]
> Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) will be used to resolve the asynchronous processing requirements, offering a lightweight standard way to manage concurrent notifications without introducing heavyweight dependencies.

## Proposed Changes

---

### Core Domain (`com.nova.domain`)

The domain layer hosts the core business models, the Result pattern encapsulation, and the port interfaces.

#### [NEW] `src/main/java/com/nova/domain/result/Result.java`
- A generic wrapper for operations (`Result<T>`). We will implement this as a **Java sealed interface** (`public sealed interface Result<T> permits Success, Failure`) leveraging Java 21 pattern matching to favor functional programming style.

#### [NEW] `src/main/java/com/nova/domain/models/NotificationRequest.java`
- DTO record holding the payload, channel type, recipient metadata, and contextual data.

#### [NEW] `src/main/java/com/nova/domain/models/Recipient.java`
- DTO record containing native validation logic for formatting domains like Email strings or Phone Numbers.

#### [NEW] `src/main/java/com/nova/domain/models/ChannelType.java`
- Enum restricting formats to `EMAIL`, `SMS`, and `PUSH`.

#### [NEW] `src/main/java/com/nova/domain/models/Template.java`
- DTO record representing string format variables and template identifiers.

#### [NEW] `src/main/java/com/nova/domain/ports/NotificationProvider.java`
- Port interface representing any channel provider mapping internally to `Result<Void> send(NotificationRequest)`.

#### [NEW] `src/main/java/com/nova/domain/ports/TemplateEngine.java`
- Port interface resolving parameterized templates to physical strings.

---

### Application Layer (`com.nova.application`)

This layer orchestrates domain models, applies contextual logic, and manages synchronous/asynchronous flows.

#### [NEW] `src/main/java/com/nova/application/services/NotificationService.java`
- Primary entry interface exposing:
    - `Result<Void> sendSync(NotificationRequest request)`
    - `CompletableFuture<Result<Void>> sendAsync(NotificationRequest request)`

#### [NEW] `src/main/java/com/nova/application/services/NotificationServiceImpl.java`
- Implements `NotificationService`. Handles param validation, parses templates, tracks latency with SLF4J (duration timers), and dispatches messages via the underlying Strategy using Java Virtual Threads for async dispatching.

#### [NEW] `src/main/java/com/nova/application/strategies/NotificationStrategy.java`
- Strategy pattern interface for processing channel-specific routing.

#### [NEW] `src/main/java/com/nova/application/strategies/EmailStrategy.java`
#### [NEW] `src/main/java/com/nova/application/strategies/SmsStrategy.java`
#### [NEW] `src/main/java/com/nova/application/strategies/PushStrategy.java`
- Concrete strategy implementations filtering logic, encapsulating channels to separate providers.

---

### Infrastructure Layer (`com.nova.infrastructure`)

Handles external communication, configuration properties, masked logging, and native dependencies contexts.

#### [NEW] `src/main/java/com/nova/infrastructure/providers/StubEmailProvider.java`
#### [NEW] `src/main/java/com/nova/infrastructure/providers/StubSmsProvider.java`
#### [NEW] `src/main/java/com/nova/infrastructure/providers/StubPushProvider.java`
- Concrete implementations of `NotificationProvider` for Email, SMS, and Push. *Per feedback, these will be mocked/stubbed and will simply emulate connections without executing real API calls to external services.*

#### [NEW] `src/main/java/com/nova/infrastructure/resilience/RetryOptions.java`
#### [NEW] `src/main/java/com/nova/infrastructure/resilience/Resilience4jRetryDecorator.java`
- Integrates the **Resilience4j Retry module**. A decorator implementing `NotificationProvider` that wraps concrete providers to manage configurable automated retries via Resilience4j functionalities.

#### [NEW] `src/main/java/com/nova/infrastructure/logging/PrivacyMaskingLogger.java`
- A utility/wrapper relying on standard SLF4J, performing regex analysis to redact sensitive records (Emails, Tokens, Phone Numbers) preventing PII leakage.

#### [NEW] `src/main/java/com/nova/infrastructure/config/NotifyBuilder.java`
- The core root Builder bypassing `application.properties` to cleanly construct and wire DI dependencies, strategies, Resilience4j configuration, and logger programmatically via code.

---

## Verification Plan

### Automated Tests
- Unit testing domain models validating Email and Phone constraints strictly.
- Validating the Strategy components without any physical I/O (using Mockito) to simulate real connections seamlessly.
- Testing the `PrivacyMaskingLogger` to ensure identifiers correctly mask (e.g. `jo*************@gmail.com`).
- Verify Async handling using Java Virtual Threads doesn't block the caller unexpectedly.

### Manual Verification
- Stand up a simple `main()` implementation using `NotifyBuilder.builder()...build()` to launch mock synchronous and asynchronous channel payloads.
