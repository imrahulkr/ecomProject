package com.ecommerce.project.notification.email.provider;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;

public interface EmailProvider {
    EmailSendResult sendEmail(EmailRequest emailRequest);
    String getProviderName();
}
