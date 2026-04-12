package com.nova.domain.models;

import java.util.Map;
import java.util.Optional;

public record Template(
        String templateId,
        Map<String, String> variables
) {
    public Template {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("Template ID cannot be null or blank");
        }
    }
    
    public Optional<Map<String, String>> getVariables() {
        return Optional.ofNullable(variables);
    }
}
