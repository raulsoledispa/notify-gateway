package com.nova.application.services;

import com.nova.application.strategies.NotificationStrategy;
import com.nova.domain.models.ChannelType;
import com.nova.domain.models.EmailContact;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.PushContact;
import com.nova.domain.models.RecipientContact;
import com.nova.domain.models.SmsContact;
import com.nova.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private NotificationService service;
    private NotificationStrategy mockEmailStrategy;
    private NotificationStrategy mockSmsStrategy;
    private NotificationStrategy mockPushStrategy;

    @BeforeEach
    void setUp() {
        mockEmailStrategy = mock(NotificationStrategy.class);
        when(mockEmailStrategy.getChannelType()).thenReturn(ChannelType.EMAIL);

        mockSmsStrategy = mock(NotificationStrategy.class);
        when(mockSmsStrategy.getChannelType()).thenReturn(ChannelType.SMS);

        mockPushStrategy = mock(NotificationStrategy.class);
        when(mockPushStrategy.getChannelType()).thenReturn(ChannelType.PUSH);

        com.nova.domain.ports.TemplateEngine mockEngine = mock(com.nova.domain.ports.TemplateEngine.class);
        when(mockEngine.resolve(any())).thenReturn("Resolved Mock Body");

        service = new NotificationServiceImpl(List.of(mockEmailStrategy, mockSmsStrategy, mockPushStrategy), mockEngine);
    }

    // --- EMAIL ---
    @Test
    void testSendSyncEmail_Success() {
        NotificationRequest request = createRequest(new EmailContact("test@example.com"));
        when(mockEmailStrategy.execute(any(NotificationRequest.class))).thenReturn(new Result.Success<>(null));

        Result<Void> result = service.sendSync(request);

        assertInstanceOf(Result.Success.class, result);
        verify(mockEmailStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    @Test
    void testSendAsyncEmail_Success() throws Exception {
        NotificationRequest request = createRequest(new EmailContact("test@example.com"));
        when(mockEmailStrategy.execute(any(NotificationRequest.class))).thenReturn(new Result.Success<>(null));

        CompletableFuture<Result<Void>> future = service.sendAsync(request);
        Result<Void> result = future.get();

        assertInstanceOf(Result.Success.class, result);
        verify(mockEmailStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    // --- SMS ---
    @Test
    void testSendSyncSms_ProviderFailureResponse() {
        NotificationRequest request = createRequest(new SmsContact("+1234567890"));
        when(mockSmsStrategy.execute(any(NotificationRequest.class))).thenReturn(new Result.Failure<>("Twilio API Invalid Token", null));

        Result<Void> result = service.sendSync(request);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals("Twilio API Invalid Token", ((Result.Failure<Void>) result).message());
        verify(mockSmsStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    @Test
    void testSendAsyncSms_ProviderFailureResponse() throws Exception {
        NotificationRequest request = createRequest(new SmsContact("+1234567890"));
        when(mockSmsStrategy.execute(any(NotificationRequest.class))).thenReturn(new Result.Failure<>("Twilio API Rate Limited", null));

        CompletableFuture<Result<Void>> future = service.sendAsync(request);
        Result<Void> result = future.get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals("Twilio API Rate Limited", ((Result.Failure<Void>) result).message());
        verify(mockSmsStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    // --- PUSH ---
    @Test
    void testSendSyncPush_ProviderThrowsException() {
        NotificationRequest request = createRequest(new PushContact("token_12345"));
        when(mockPushStrategy.execute(any(NotificationRequest.class))).thenThrow(new RuntimeException("Connection Timeout"));

        Result<Void> result = service.sendSync(request);

        assertInstanceOf(Result.Failure.class, result);
        assertTrue(((Result.Failure<Void>) result).message().contains("Unexpected error: Connection Timeout"));
        verify(mockPushStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    @Test
    void testSendAsyncPush_ProviderThrowsException() throws Exception {
        NotificationRequest request = createRequest(new PushContact("token_12345"));
        when(mockPushStrategy.execute(any(NotificationRequest.class))).thenThrow(new RuntimeException("SSL Handshake Exception"));

        CompletableFuture<Result<Void>> future = service.sendAsync(request);
        Result<Void> result = future.get();

        assertInstanceOf(Result.Failure.class, result);
        assertTrue(((Result.Failure<Void>) result).message().contains("Unexpected error: SSL Handshake Exception"));
        verify(mockPushStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    // --- TEMPLATE ---
    @Test
    void testSendSyncEmail_WithTemplate() {
        com.nova.domain.models.Template template = new com.nova.domain.models.Template("welcome_template", java.util.Map.of("name", "John"));
        NotificationRequest request = NotificationRequest.builder()
                .contact(new EmailContact("test@example.com"))
                .template(template)
                .build();

        when(mockEmailStrategy.execute(any(NotificationRequest.class))).thenReturn(new Result.Success<>(null));

        Result<Void> result = service.sendSync(request);

        assertInstanceOf(Result.Success.class, result);
        verify(mockEmailStrategy, times(1)).execute(any(NotificationRequest.class));
    }

    private NotificationRequest createRequest(RecipientContact contact) {
        return NotificationRequest.builder()
                .contact(contact)
                .plainTextBody("Mock Message Payload")
                .build();
    }
}
