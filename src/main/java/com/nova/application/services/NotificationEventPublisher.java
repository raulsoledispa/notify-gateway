package com.nova.application.services;

import com.nova.domain.models.NotificationEvent;
import com.nova.domain.ports.NotificationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final List<NotificationEventListener> listeners;

    public NotificationEventPublisher(List<NotificationEventListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public static NotificationEventPublisher empty() {
        return new NotificationEventPublisher(List.of());
    }

    public void publish(NotificationEvent event) {
        for (NotificationEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Event listener failed for event [{}]: {}", event.status(), e.getMessage(), e);
            }
        }
    }
}
