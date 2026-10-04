package com.ecommerce.project.notification.email.provider.noop;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "noop")
public class NoOpEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailProvider.class);

    @Override
    public EmailSendResult sendEmail(EmailRequest emailRequest) {
        log.info("[NO-OP] would send email to {}, {}", emailRequest.getEmailType(), emailRequest.getTo());
        return EmailSendResult.builder()
                .success(true)
                .providerMessageId("test-" + UUID.randomUUID().toString())
                .providerUsed("noop")
                .build();
    }

    @Override
    public String getProviderName() {
        return "noop";
    }
}
