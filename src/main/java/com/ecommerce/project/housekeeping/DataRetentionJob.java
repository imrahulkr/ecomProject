package com.ecommerce.project.housekeeping;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataRetentionJob {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionJob.class);

    private final DataRetentionService dataRetentionService;

    @Scheduled(cron = "${app.retention.cron}")
    public void run() {
        try {
            Map<String, Integer> deleted = dataRetentionService.purgeExpired();
            logger.info("Data retention sweep removed {}", deleted);
        } catch (Exception e) {
            logger.error("Data retention sweep failed", e);
        }
    }
}
