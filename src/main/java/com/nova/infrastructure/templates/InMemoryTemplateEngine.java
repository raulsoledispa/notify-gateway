package com.nova.infrastructure.templates;

import com.nova.domain.models.Template;
import com.nova.domain.ports.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class InMemoryTemplateEngine implements TemplateEngine {
    private static final Logger log = LoggerFactory.getLogger(InMemoryTemplateEngine.class);
    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public void registerTemplate(String id, String content) {
        templates.put(id, content);
    }

    @Override
    public String resolve(Template template) {
        String content = templates.get(template.templateId());
        if (content == null) {
            log.warn("Template with ID '{}' not found in registry", template.templateId());
            return "";
        }
        
        if (template.getVariables().isPresent()) {
            for (Map.Entry<String, String> entry : template.getVariables().get().entrySet()) {
                content = content.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return content;
    }
}
