package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.notification.email.model.EmailPriority;
import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailType;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import com.ecommerce.project.notification.email.template.EmailTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final EmailTemplateEngine templateEngine;
    private final EmailRetryService emailRetryService;

    @Value("${app.email.default-from}")
    private String defaultFrom;
    @Value("${app.base.url}")
    private String baseUrl;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailServiceImpl(EmailTemplateEngine templateEngine, EmailRetryService emailRetryService) {
        this.templateEngine = templateEngine;
        this.emailRetryService = emailRetryService;
    }

    @Override
    public void sendVerificationEmail(User user, String token) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "verificationUrl",  baseUrl +"/api/auth/verify?token=" + token
        );
        logger.debug("Sending verification email to name={}, url={}", vars.get("name"), vars.get("verificationUrl"));
        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Verify your account")
                .htmlBody(templateEngine.render("verification-email", vars))
                .emailType(EmailType.VERIFICATION)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "verificationUrl",  baseUrl +"/reset-password?token=" + token
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Reset your Password")
                .htmlBody(templateEngine.render("password-reset", vars))
                .emailType(EmailType.PASSWORD_RESET)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendPasswordChangedEmail(User user) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername()
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Reset your Password")
                .htmlBody(templateEngine.render("password-changed", vars))
                .emailType(EmailType.PASSWORD_RESET)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendOrderConfirmation(Order order) {

    }

    @Override
    public void sendShippingUpdate(Order order, String trackingNumber) {

    }

    @Override
    public void sendAbandonedCartReminder(User user, Cart cart) {

    }
}
