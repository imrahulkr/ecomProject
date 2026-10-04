package com.ecommerce.project.notification.email.provider.resend;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);
    private final Resend resendClient;

    public ResendEmailProvider(Resend resendClient) {
        this.resendClient = resendClient;
    }

    @Override
    public EmailSendResult sendEmail(EmailRequest emailRequest) {
        try {

            CreateEmailOptions.Builder optionsBuilder = CreateEmailOptions.builder()
                    .from(emailRequest.getFrom())
                    .to(emailRequest.getTo())
                    .subject(emailRequest.getSubject())
                    .html(emailRequest.getHtmlBody());

            if( emailRequest.getTextBody() != null && !emailRequest.getTextBody().isEmpty() ) {
                optionsBuilder.text(emailRequest.getTextBody());
            }

            CreateEmailResponse response = resendClient.emails().send(optionsBuilder.build());

            return EmailSendResult.builder()
                    .success(true)
                    .providerMessageId(response.getId())
                    .providerUsed("resend")
                    .build();
        } catch (ResendException e) {
            log.error("Resend send failed for {} : {}", emailRequest.getTo(), e.getMessage());

            return EmailSendResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .providerUsed("resend")
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return "resend";
    }
}
