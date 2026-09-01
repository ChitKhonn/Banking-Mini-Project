package com.bank.api.scheduler;

import com.bank.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCleanupScheduler {

    private final TransactionRepository transactionRepository;

    @Value("${transaction.cleanup.retention-days:3}")
    private int retentionDays;

    @Scheduled(cron = "${transaction.cleanup.cron:0 0 0 * * *}")
    public void deleteOldTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("Running scheduled cleanup: deleting transactions created before {}", cutoff);

        long deletedCount = transactionRepository.deleteByCreatedAtBefore(cutoff);

        log.info("Scheduled cleanup complete: {} transaction(s) deleted", deletedCount);
    }
}
