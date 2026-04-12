package com.nova.application.strategies;

import com.nova.domain.models.NotificationRequest;
import com.nova.domain.models.ChannelType;
import com.nova.domain.result.Result;

public interface NotificationStrategy {
    Result<Void> execute(NotificationRequest request);
    ChannelType getChannelType();
}
