package com.nova.application.strategies;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.EmailContact;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailStrategyTest {

    @Test
    void testExecuteWithProviders() {
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        when(mockProvider.getSupportedChannel()).thenReturn(ChannelType.EMAIL);
        when(mockProvider.send(any())).thenReturn(new Result.Success<>(null));

        EmailStrategy strategy = new EmailStrategy(List.of(mockProvider));

        NotificationRequest request = NotificationRequest.builder()
                .contact(new EmailContact("test@test.com"))
                .build();

        Result<Void> result = strategy.execute(request);
        assertTrue(result instanceof Result.Success);
        verify(mockProvider, times(1)).send(request);
    }

    @Test
    void testExecuteWithoutProviders() {
        EmailStrategy strategy = new EmailStrategy(Collections.emptyList());

        NotificationRequest request = NotificationRequest.builder()
                .contact(new EmailContact("test@test.com"))
                .build();

        Result<Void> result = strategy.execute(request);
        assertTrue(result instanceof Result.Failure);
        assertEquals("No providers configured for EMAIL channel", ((Result.Failure<Void>) result).message());
    }
}
