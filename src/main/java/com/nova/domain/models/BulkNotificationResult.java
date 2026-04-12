package com.nova.domain.models;

import java.util.List;

public record BulkNotificationResult(
        List<RecipientContact> successes,
        List<BulkFailureDetail> failures
) {
    public record BulkFailureDetail(RecipientContact contact, String errorMessage) {}
}
