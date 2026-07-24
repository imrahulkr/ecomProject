package com.ecommerce.project.notification.email.template;

import com.ecommerce.project.notification.email.exception.TemplateRenderException;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Component
public class ThymeleafTemplateEngine implements EmailTemplateEngine{

    private final SpringTemplateEngine templateEngine;

    public ThymeleafTemplateEngine(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(String templateName, Map<String, Object> variable) {
        Context context = new Context();
        context.setVariables(variable);
        try {
            return templateEngine.process("email/"+templateName, context);
        } catch (Exception e) {
            throw new TemplateRenderException("Falied to render template : " + templateName, e);
        }
    }
}
