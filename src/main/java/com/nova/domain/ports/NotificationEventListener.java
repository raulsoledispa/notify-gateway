package com.nova.domain.ports;

import com.nova.domain.models.NotificationEvent;

@FunctionalInterface
public interface NotificationEventListener {
    void onEvent(NotificationEvent event);
}
