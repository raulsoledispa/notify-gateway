package com.nova.domain.ports;

import com.nova.domain.models.Template;

public interface TemplateEngine {
    /**
     * Resolves a template to a concrete string payload.
     */
    String resolve(Template template);
}
