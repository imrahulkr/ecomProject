package com.ecommerce.project.notification.email.repository;

import com.ecommerce.project.notification.email.model.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<EmailLog> findByStatus(EmailStatus status);
}
