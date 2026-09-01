package com.bank.api.service;

import com.bank.api.entity.Transaction;
import com.bank.api.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class EmailService {

    @Async("emailTaskExecutor")
    public void sendDepositEmail(User owner, Transaction txn) {
        log.info("[MOCK EMAIL] To: {} | Subject: Deposit Successful | " +
                        "Your deposit of {} was successful. New balance: {}",
                owner.getEmail(), txn.getAmount(), txn.getBalanceAfter());
    }

    @Async("emailTaskExecutor")
    public void sendWithdrawEmail(User owner, Transaction txn) {
        log.info("[MOCK EMAIL] To: {} | Subject: Withdrawal Successful | " +
                        "Your withdrawal of {} was successful. New balance: {}",
                owner.getEmail(), txn.getAmount(), txn.getBalanceAfter());
    }

    @Async("emailTaskExecutor")
    public void sendTransferEmails(User sender, User receiver, Transaction txn) {
        log.info("[MOCK EMAIL] To: {} | Subject: Transfer Sent | " +
                        "You sent {} to {}. New balance: {}",
                sender.getEmail(), txn.getAmount(), receiver.getEmail(), txn.getFromBalanceAfter());

        log.info("[MOCK EMAIL] To: {} | Subject: Transfer Received | " +
                        "You received {} from {}. New balance: {}",
                receiver.getEmail(), txn.getAmount(), sender.getEmail(), txn.getToBalanceAfter());
    }
}
