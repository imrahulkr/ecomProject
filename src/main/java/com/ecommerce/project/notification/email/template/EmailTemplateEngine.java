package com.ecommerce.project.notification.email.template;

import java.util.Map;

public interface EmailTemplateEngine {
    String render(String templateName, Map<String, Object> variable);
}
