package com.nova.domain.ports;

import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.ChannelType;
import com.nova.domain.result.Result;

public interface NotificationProvider {
    /**
     * Send a notification synchronously.
     */
    Result<Void> send(NotificationRequest request);

    /**
     * Identifies which channel this provider supports.
     */
    ChannelType getSupportedChannel();
}
