package com.nova.application.services;

import com.nova.domain.models.NotificationEvent;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.NotificationStatus;
import com.nova.domain.models.EmailContact;
import com.nova.domain.ports.NotificationEventListener;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationEventPublisherTest {

    private NotificationEvent createTestEvent() {
        return NotificationEvent.builder()
                .status(NotificationStatus.SUCCESS)
                .request(NotificationRequest.builder()
                        .contact(new EmailContact("test@example.com"))
                        .plainTextBody("Test")
                        .build())
                .message("Test event")
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void testPublishCallsAllListeners() {
        NotificationEventListener listener1 = mock(NotificationEventListener.class);
        NotificationEventListener listener2 = mock(NotificationEventListener.class);

        NotificationEventPublisher publisher = new NotificationEventPublisher(List.of(listener1, listener2));
        NotificationEvent event = createTestEvent();

        publisher.publish(event);

        verify(listener1, times(1)).onEvent(event);
        verify(listener2, times(1)).onEvent(event);
    }

    @Test
    void testFailingListenerDoesNotBlockOthers() {
        NotificationEventListener failingListener = mock(NotificationEventListener.class);
        doThrow(new RuntimeException("Listener error")).when(failingListener).onEvent(any());
        NotificationEventListener healthyListener = mock(NotificationEventListener.class);

        NotificationEventPublisher publisher = new NotificationEventPublisher(List.of(failingListener, healthyListener));
        NotificationEvent event = createTestEvent();

        assertDoesNotThrow(() -> publisher.publish(event));
        verify(healthyListener, times(1)).onEvent(event);
    }

    @Test
    void testEmptyPublisherDoesNotThrow() {
        NotificationEventPublisher publisher = NotificationEventPublisher.empty();
        assertDoesNotThrow(() -> publisher.publish(createTestEvent()));
    }
}
