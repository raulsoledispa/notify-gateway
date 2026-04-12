package com.nova.application.services;

import com.nova.application.strategies.NotificationStrategy;
import com.nova.domain.models.ChannelType;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.result.Result;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StrategyResolver {

    private final Map<ChannelType, NotificationStrategy> strategyMap;

    public StrategyResolver(List<NotificationStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotificationStrategy::getChannelType, s -> s));
    }

    public Result<NotificationStrategy> resolve(NotificationRequest request) {
        NotificationStrategy strategy = strategyMap.get(request.channelType());
        if (strategy == null) {
            return new Result.Failure<>("No strategy found for channel: " + request.channelType());
        }
        return new Result.Success<>(strategy);
    }
}
