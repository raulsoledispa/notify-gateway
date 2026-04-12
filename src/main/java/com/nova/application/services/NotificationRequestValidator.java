package com.nova.application.services;

import com.nova.domain.models.NotificationRequest;
import com.nova.domain.result.Result;

public class NotificationRequestValidator {

    public Result<Void> validate(NotificationRequest request) {
        if (request == null || request.contact() == null) {
            return new Result.Failure<>("Invalid request or contact missing");
        }
        return new Result.Success<>(null);
    }
}
