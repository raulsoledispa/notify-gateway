package com.nova.application.services;

import com.nova.domain.models.NotificationRequest;
import com.nova.domain.ports.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateProcessor {

    private static final Logger log = LoggerFactory.getLogger(TemplateProcessor.class);

    private final TemplateEngine templateEngine;

    public TemplateProcessor(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public NotificationRequest process(NotificationRequest request) {
        if (request.getTemplate().isEmpty() || templateEngine == null) {
            return request;
        }

        String resolvedBody = templateEngine.resolve(request.template());
        log.info("Template successfully resolved into formatted plaintext body.");

        return NotificationRequest.builder()
                .contact(request.contact())
                .plainTextBody(resolvedBody)
                .template(request.template())
                .build();
    }
}
