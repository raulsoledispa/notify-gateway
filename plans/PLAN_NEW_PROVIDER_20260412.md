# Plan: notify-gateway v2 Features

## Context

The notify-gateway is a Java 21 notification library using hexagonal architecture. It currently supports Email (SendGrid), SMS (Twilio), and Push (Firebase) channels via the Strategy pattern. The spec_v2 requires three new capabilities: a **Slack provider**, **bulk notifications**, and **status events** for observability.

---

## Feature 1: Slack Provider

Follow the exact patterns of Email/SendGrid to add a fourth notification channel.

### Domain Layer

1. **Modify `ChannelType.java`** — Add `SLACK` enum constant
    - `src/main/java/com/nova/domain/models/ChannelType.java`

2. **Create `SlackContact.java`** — Record implementing `RecipientContact`
    - `src/main/java/com/nova/domain/models/SlackContact.java`
    - Validates webhook URL (non-blank, starts with `https://hooks.slack.com/`)
    - Returns `ChannelType.SLACK` from `channelType()`
    - Pattern: follows `EmailContact.java`

3. **Modify `RecipientContact.java`** — Add `SlackContact` to permits clause
    - `src/main/java/com/nova/domain/models/RecipientContact.java`

### Application Layer

4. **Create `SlackStrategy.java`**
    - `src/main/java/com/nova/application/strategies/SlackStrategy.java`
    - Pattern: identical to `EmailStrategy.java` — filters providers by `ChannelType.SLACK`

### Infrastructure Layer

5. **Create `SlackConfig.java`** — Config record
    - `src/main/java/com/nova/infrastructure/config/SlackConfig.java`
    - Fields: `webhookUrl` (mandatory). Validates non-blank.
    - Pattern: follows `SendGridConfig.java`

6. **Create `SlackProvider.java`** — Provider implementation
    - `src/main/java/com/nova/infrastructure/providers/SlackProvider.java`
    - Uses `PrivacyMaskingLogger`, validates contact is `SlackContact`
    - Stub implementation (no real HTTP call), returns `Result.Success`
    - Pattern: follows `SendGridEmailProvider.java`

7. **Modify `NotifyBuilder.java`** — Wire Slack into the builder
    - Add `withSlack(SlackConfig config)` method
    - Add `SlackStrategy` to the strategies list in `build()`

---

## Feature 2: Bulk Notifications

> **Key constraint:** A bulk request sends the **same message** to multiple recipients of the **same channel type**. It does NOT allow mixing different notification types (e.g., email + SMS) in a single request.

### Domain Layer

8. **Create `BulkNotificationRequest.java`** — Record with `@Builder`
    - `src/main/java/com/nova/domain/models/BulkNotificationRequest.java`
    - Fields: `List<RecipientContact> contacts`, `String plainTextBody`, `Template template`
    - Compact constructor validates: contacts non-null, non-empty, and **all contacts share the same `channelType()`** (reject mixed types)
    - Derives `channelType()` from the first contact

9. **Create `BulkNotificationResult.java`** — Record for results
    - `src/main/java/com/nova/domain/models/BulkNotificationResult.java`
    - Fields: `List<RecipientContact> successes`, `List<BulkFailureDetail> failures`
    - Nested record: `BulkFailureDetail(RecipientContact contact, String errorMessage)`

### Application Layer

10. **Modify `NotificationService.java`** — Add single bulk method
    - `BulkNotificationResult sendBulk(BulkNotificationRequest request)`
    - One method only (no async variant)

11. **Modify `NotificationServiceImpl.java`** — Implement `sendBulk`
    - Builds a `NotificationRequest` per contact (same body/template, different contact)
    - Launches each via `CompletableFuture.supplyAsync` on the existing virtual thread executor, calls `sendSync` per request
    - Joins all, partitions results into success/failure lists keyed by contact
    - Each send is independent — one failure does not stop others
    - Logs total time for the bulk operation

---

## Feature 3: Status Events

> **Event emission points:** Events are generated at **two levels** — the service layer (PENDING, SUCCESS, FAILURE) and the retry decorator (RETRY). Providers themselves do NOT emit events; the decorator wraps each provider and observes its results.
>
> ```
> Service: PENDING → Decorator wraps provider call → 
>   Provider returns Failure → Decorator: RETRY (attempt 2) → Provider retried →
>   Result bubbles up → Service: SUCCESS or FAILURE
> ```

### Domain Layer

12. **Create `NotificationStatus.java`** — Enum
    - `src/main/java/com/nova/domain/models/NotificationStatus.java`
    - Values: `PENDING`, `RETRY`, `SUCCESS`, `FAILURE`
    - `RETRY` is emitted from the `Resilience4jRetryDecorator` each time a send is retried (includes attempt number in the event message)

13. **Create `NotificationEvent.java`** — Record with `@Builder`
    - `src/main/java/com/nova/domain/models/NotificationEvent.java`
    - Fields: `NotificationStatus status`, `NotificationRequest request`, `String message`, `Instant timestamp`

14. **Create `NotificationEventListener.java`** — Outbound port (functional interface)
    - `src/main/java/com/nova/domain/ports/NotificationEventListener.java`
    - Single method: `void onEvent(NotificationEvent event)`

### Application Layer

15. **Create `NotificationEventPublisher.java`**
    - `src/main/java/com/nova/application/services/NotificationEventPublisher.java`
    - Holds `List<NotificationEventListener>`, iterates and calls each on `publish()`
    - Try-catch per listener so one failure doesn't block others
    - Static factory `empty()` for no-listener default

16. **Modify `NotificationServiceImpl.java`** — Emit events
    - Add `NotificationEventPublisher` field, injected via constructor
    - Keep backward-compatible 2-arg constructor defaulting to `NotificationEventPublisher.empty()`
    - Emit `PENDING` after validation passes, before dispatch
    - Emit `SUCCESS` on `Result.Success`
    - Emit `FAILURE` on `Result.Failure` or exception

### Infrastructure Layer

17. **Modify `Resilience4jRetryDecorator.java`** — Emit RETRY events
    - Add optional `NotificationEventPublisher` field
    - New 3-arg constructor: `(provider, retry, eventPublisher)`
    - Existing 2-arg constructor defaults publisher to `null`
    - Track attempts with `AtomicInteger` inside `send()` — on attempt > 1, publish `RETRY` event with attempt number

18. **Modify `NotifyBuilder.java`** — Wire event listeners
    - Add `List<NotificationEventListener>` field
    - Add `withEventListener(NotificationEventListener listener)` method
    - In `build()`: create `NotificationEventPublisher` from listeners, pass to `Resilience4jRetryDecorator` (3-arg) and `NotificationServiceImpl`

---

## Tests

19. **Create `SlackContactTest.java`** — Domain validation tests
    - `src/test/java/com/nova/domain/models/SlackContactTest.java`

20. **Create `SlackStrategyTest.java`** — Strategy routing tests
    - `src/test/java/com/nova/application/strategies/SlackStrategyTest.java`

21. **Modify `NotificationServiceImplTest.java`** — Bulk + event tests
    - Bulk: all-success, partial-failure, all-failure, independence guarantee, **reject mixed channel types**
    - Events: PENDING+SUCCESS emitted on success, FAILURE emitted on failure

22. **Create `NotificationEventPublisherTest.java`** — Publisher tests
    - All listeners called, failing listener doesn't block others, empty publisher is safe

---

## Documentation

23. **Update `README.md`** — Add Slack provider, bulk, and events documentation
    - Update features list to include Slack, Bulk, and Status Events
    - Add Slack row to the Supported Providers table
    - Update Quick Start config example to include `.withSlack()` and `.withEventListener()`
    - Add new usage section: **4. Sending a Slack Notification** (webhook URL contact, send example)
    - Add new usage section: **5. Sending Bulk Notifications** (same message to multiple email recipients, handle `BulkNotificationResult`)
    - Add new usage section: **6. Listening to Status Events** (register a listener via builder, log/react to PENDING, RETRY, SUCCESS, FAILURE events)
    - Update API Reference table: add `SlackContact` to `RecipientContact` permits, add `SLACK` to `ChannelType`, add `BulkNotificationRequest`/`BulkNotificationResult`, add `NotificationEventListener`

---

## Implementation Order

```
Domain models (steps 1-3, 8-9, 12-14)  — no dependencies
    ↓
Application layer (steps 4, 10-11, 15-16)  — depends on domain
    ↓
Infrastructure layer (steps 5-7, 17-18)  — depends on domain + application
    ↓
Tests (steps 19-22)  — after production code
    ↓
Documentation (step 23)  — after all code is complete
```

## Verification

1. `mvn compile` — ensure all new code compiles
2. `mvn test` — all existing + new tests pass
3. Manual verification: write a small main method using `NotifyBuilder` to:
    - Send a single Slack notification
    - Send a bulk notification to multiple recipients of the same channel
    - Register an event listener and verify PENDING/SUCCESS/RETRY events are printed

---

## Key Considerations

1. **Bulk = same message, same channel, multiple recipients.** The `BulkNotificationRequest` enforces that all contacts share the same `channelType()` at construction time. This prevents callers from accidentally mixing email and SMS in one bulk call.
2. **Single `sendBulk` method** on `NotificationService` — no async variant. Internally uses virtual threads for parallel execution.
3. **RETRY event** is emitted by `Resilience4jRetryDecorator`, not by the service layer. This keeps retry awareness in the infrastructure where the retry mechanism lives. The event includes the attempt number so clients can track retry depth.
4. **Providers do NOT emit events.** The decorator wraps each provider and observes `Result.Failure` to trigger retries and RETRY events. The service layer observes the final result to emit SUCCESS/FAILURE. This avoids coupling providers to the event system.
5. **Status events are synchronous and best-effort.** A failing listener does not block other listeners or the notification itself. Clients wanting async event handling implement that in their own listener.
6. **Backward compatibility.** Existing 2-arg constructors for `NotificationServiceImpl` and `Resilience4jRetryDecorator` continue to work with no event publishing.
