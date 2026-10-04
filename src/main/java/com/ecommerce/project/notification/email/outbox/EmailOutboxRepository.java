package com.ecommerce.project.notification.email.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxMessage, Long> {

    // FOR UPDATE SKIP LOCKED (lock timeout -2 is Hibernate's SKIP_LOCKED): several instances can
    // poll at once without double-sending - each claims a disjoint set of rows. Also picks up
    // rows stuck in SENDING from a sender that died mid-send.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT m FROM EmailOutboxMessage m WHERE "
            + "(m.status = com.ecommerce.project.notification.email.outbox.EmailOutboxStatus.PENDING AND m.nextAttemptAt <= :now) "
            + "OR (m.status = com.ecommerce.project.notification.email.outbox.EmailOutboxStatus.SENDING AND m.updatedAt < :staleBefore) "
            + "ORDER BY m.nextAttemptAt")
    List<EmailOutboxMessage> findDueForUpdate(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore, Pageable limit);
}
