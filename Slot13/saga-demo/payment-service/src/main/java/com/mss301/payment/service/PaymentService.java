package com.mss301.payment.service;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.Payment;
import com.mss301.payment.entity.PaymentStatus;
import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment pay(PaymentRequest req) {

        var existing = paymentRepository.findById(req.orderId());

        if (existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = walletRepository.findById(req.customerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy ví của " + req.customerId()
                ));

        if (wallet.getBalance() < req.amount()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Số dư không đủ: còn " + wallet.getBalance()
                            + ", cần " + req.amount()
            );
        }

        wallet.setBalance(
                wallet.getBalance() - req.amount()
        );

        Payment payment = new Payment();
        payment.setOrderId(req.orderId());
        payment.setCustomerId(req.customerId());
        payment.setAmount(req.amount());
        payment.setStatus(PaymentStatus.PAID);

        log.info(
                "PAY order={} customer={} amount={} -> balance còn {}",
                req.orderId(),
                req.customerId(),
                req.amount(),
                wallet.getBalance()
        );

        return paymentRepository.save(payment);
    }

    @Transactional
    public void refund(Long orderId) {

        var opt = paymentRepository.findById(orderId);

        if (opt.isEmpty()
                || opt.get().getStatus() == PaymentStatus.REFUNDED) {

            log.info(
                    "REFUND order={} -> không có gì để hoàn tác",
                    orderId
            );

            return;
        }

        Payment payment = opt.get();

        Wallet wallet = walletRepository
                .findById(payment.getCustomerId())
                .orElseThrow();

        wallet.setBalance(
                wallet.getBalance() + payment.getAmount()
        );

        payment.setStatus(PaymentStatus.REFUNDED);

        log.info(
                "REFUND order={} -> hoàn {} cho {}, balance = {}",
                orderId,
                payment.getAmount(),
                payment.getCustomerId(),
                wallet.getBalance()
        );
    }
}