package com.ecommerce.project.notification.email.provider.smtp;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);
    private final JavaMailSender javaMailSender;

    public SmtpEmailProvider(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public EmailSendResult sendEmail(EmailRequest emailRequest) {
        try{
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(emailRequest.getTo());
            helper.setFrom(emailRequest.getFrom());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getTextBody());

            javaMailSender.send(message);

            return EmailSendResult.builder()
                    .success(true)
                    .providerMessageId(UUID.randomUUID().toString()) //SMTP has no native ID
                    .providerUsed("smtp")
                    .build();
        } catch (MessagingException e){

            return EmailSendResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage()) //SMTP has no native ID
                    .providerUsed("smtp")
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return "smtp";
    }
}
