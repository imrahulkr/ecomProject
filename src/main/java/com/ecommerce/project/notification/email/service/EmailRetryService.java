package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.notification.email.exception.EmailSendException;
import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import com.ecommerce.project.notification.email.repository.EmailLog;
import com.ecommerce.project.notification.email.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailRetryService {
    private static  final Logger LOGGER = LoggerFactory.getLogger(EmailRetryService.class.getName());
    private final EmailProvider emailProvider;
    private final EmailLogRepository emailLogRepository;

    public EmailRetryService(EmailProvider emailProvider, EmailLogRepository emailLogRepository) {
        this.emailProvider = emailProvider;
        this.emailLogRepository = emailLogRepository;
    }

    @Async("emailExecutor")
    @Retryable(
            retryFor = EmailSendException.class,
            maxAttemptsExpression = "${app.email.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${app.email.retry.initial-delay-ms}",
                    multiplierExpression = "${app.email.retry.multiplier}"
            )
    )
    public void sendEmailWithRetry(EmailRequest request) {
        EmailSendResult result = emailProvider.sendEmail(request);

        if(!result.isSuccess()){
            throw new EmailSendException(result.getErrorMessage());
        }
        emailLogRepository.save(EmailLog.from(request, result));
        LOGGER.info("Email sent successfully to {} via {}", request.getFrom(), result.getProviderUsed());
    }

    @Recover
    public void recover(EmailSendException e, EmailRequest request){
        LOGGER.error("Email permanently failed for {} after retries : {}", request.getTo(), e.getMessage());
        EmailSendResult failedResult = EmailSendResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .providerUsed(emailProvider.getProviderName())
                .build();
        emailLogRepository.save(EmailLog.from(request, failedResult));
    }
}
