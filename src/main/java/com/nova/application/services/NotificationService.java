package com.nova.application.services;

import com.nova.domain.models.BulkNotificationRequest;
import com.nova.domain.models.BulkNotificationResult;
import com.nova.domain.models.NotificationRequest;
import com.nova.domain.result.Result;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    Result<Void> sendSync(NotificationRequest request);
    CompletableFuture<Result<Void>> sendAsync(NotificationRequest request);
    BulkNotificationResult sendBulk(BulkNotificationRequest request);
}
