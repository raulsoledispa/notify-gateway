package com.nova.application.strategies;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.SlackContact;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlackStrategyTest {

    @Test
    void testExecuteWithProviders() {
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        when(mockProvider.getSupportedChannel()).thenReturn(ChannelType.SLACK);
        when(mockProvider.send(any())).thenReturn(new Result.Success<>(null));

        SlackStrategy strategy = new SlackStrategy(List.of(mockProvider));

        NotificationRequest request = NotificationRequest.builder()
                .contact(new SlackContact("https://hooks.slack.com/services/T00/B00/xxxx"))
                .plainTextBody("Hello Slack")
                .build();

        Result<Void> result = strategy.execute(request);
        assertInstanceOf(Result.Success.class, result);
        verify(mockProvider, times(1)).send(request);
    }

    @Test
    void testExecuteWithoutProviders() {
        SlackStrategy strategy = new SlackStrategy(Collections.emptyList());

        NotificationRequest request = NotificationRequest.builder()
                .contact(new SlackContact("https://hooks.slack.com/services/T00/B00/xxxx"))
                .plainTextBody("Hello Slack")
                .build();

        Result<Void> result = strategy.execute(request);
        assertInstanceOf(Result.Failure.class, result);
        assertEquals("No providers configured for SLACK channel", ((Result.Failure<Void>) result).message());
    }

    @Test
    void testGetChannelType() {
        SlackStrategy strategy = new SlackStrategy(Collections.emptyList());
        assertEquals(ChannelType.SLACK, strategy.getChannelType());
    }
}
