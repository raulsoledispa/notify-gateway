package com.nova.application.strategies;

import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.NotificationProvider;
import com.nova.domain.result.Result;

import java.util.List;

public class PushStrategy implements NotificationStrategy {
    private final List<NotificationProvider> providers;

    public PushStrategy(List<NotificationProvider> allProviders) {
        this.providers = allProviders.stream()
                .filter(p -> p.getSupportedChannel() == ChannelType.PUSH)
                .toList();
    }

    @Override
    public Result<Void> execute(NotificationRequest request) {
        if (providers.isEmpty()) {
            return new Result.Failure<>("No providers configured for PUSH channel");
        }
        return providers.get(0).send(request);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.PUSH;
    }
}
