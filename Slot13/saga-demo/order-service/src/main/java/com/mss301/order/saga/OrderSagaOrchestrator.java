package com.mss301.order.saga;

import com.mss301.order.client.InventoryClient;
import com.mss301.order.client.PaymentClient;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderService orderService;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    private final RetryTemplate compensationRetry =
            new RetryTemplate(
                    RetryPolicy.builder()
                            .includes(RestClientException.class)
                            .maxRetries(2)
                            .delay(Duration.ofSeconds(1))
                            .build()
            );

    @Value("${saga.demo.payment-delay-ms:0}")
    private long paymentDelayMs;

    public Order placeOrder(CreateOrderRequest req) {

        // Bước 1: tạo Order PENDING
        Order order = orderService.createPendingOrder(req);
        Long orderId = order.getId();

        orderService.log(
                orderId,
                "CREATE_ORDER",
                "SUCCESS",
                "Order PENDING"
        );

        orderService.updateSagaStatus(
                orderId,
                SagaStatus.EXECUTING
        );

        Deque<Runnable> compensations = new ArrayDeque<>();

        try {

            // Bước 2: reserve inventory
            inventoryClient.reserve(
                    orderId,
                    req.productId(),
                    req.quantity()
            );

            orderService.log(
                    orderId,
                    "RESERVE_INVENTORY",
                    "SUCCESS",
                    "Đã giữ "
                            + req.quantity()
                            + " "
                            + req.productId()
            );

            compensations.push(
                    () -> inventoryClient.release(orderId)
            );

            // Chỉ dùng cho KB8
            if (paymentDelayMs > 0) {
                log.warn(
                        "[DEMO] Chờ {} ms trước khi thanh toán order={}",
                        paymentDelayMs,
                        orderId
                );

                sleep(paymentDelayMs);
            }

            // Bước 3: payment
            paymentClient.pay(
                    orderId,
                    req.customerId(),
                    req.amount()
            );

            orderService.log(
                    orderId,
                    "PROCESS_PAYMENT",
                    "SUCCESS",
                    "Đã thu " + req.amount()
            );

            compensations.push(
                    () -> paymentClient.refund(orderId)
            );

            // Bước 4: confirm order
            orderService.confirmOrder(orderId);

            orderService.log(
                    orderId,
                    "CONFIRM_ORDER",
                    "SUCCESS",
                    "Saga COMPLETED"
            );

        } catch (RestClientException ex) {

            String reason = extractReason(ex);

            log.warn(
                    "Saga order={} thất bại: {}",
                    orderId,
                    reason
            );

            orderService.log(
                    orderId,
                    "SAGA",
                    "FAILED",
                    reason
            );

            orderService.markFailed(
                    orderId,
                    reason
            );

            compensate(
                    orderId,
                    compensations
            );
        }

        return orderService.findById(orderId);
    }

    private void compensate(
            Long orderId,
            Deque<Runnable> compensations
    ) {

        orderService.updateSagaStatus(
                orderId,
                SagaStatus.COMPENSATING
        );

        boolean allOk = true;

        while (!compensations.isEmpty()) {

            Runnable action = compensations.pop();

            allOk &= runWithRetry(
                    orderId,
                    action
            );
        }

        if (!allOk) {

            log.error(
                    "Compensation cho order={} chưa xong, cần xử lý thủ công",
                    orderId
            );

            return;
        }

        orderService.cancelOrder(orderId);

        orderService.log(
                orderId,
                "CANCEL_ORDER",
                "SUCCESS",
                "Saga COMPENSATED"
        );
    }

    private boolean runWithRetry(
            Long orderId,
            Runnable action
    ) {

        AtomicInteger attempt =
                new AtomicInteger();

        try {

            compensationRetry.execute(() -> {

                int n = attempt.incrementAndGet();

                try {

                    action.run();

                } catch (RestClientException ex) {

                    orderService.log(
                            orderId,
                            "COMPENSATION",
                            "FAILED",
                            "Lần thử "
                                    + n
                                    + ": "
                                    + ex.getMessage()
                    );

                    throw ex;
                }

                orderService.log(
                        orderId,
                        "COMPENSATION",
                        "SUCCESS",
                        "Lần thử " + n
                );

                return null;
            });

            return true;

        } catch (RetryException ex) {

            return false;
        }
    }

    private String extractReason(
            RestClientException ex
    ) {

        if (ex instanceof RestClientResponseException r) {

            try {

                Map<?, ?> body =
                        r.getResponseBodyAs(Map.class);

                if (body != null
                        && body.get("message") != null) {

                    return r.getStatusCode().value()
                            + " - "
                            + body.get("message");
                }

            } catch (Exception ignored) {
            }

            return r.getStatusCode().toString();
        }

        return "Service không phản hồi: "
                + ex.getMessage();
    }

    private void sleep(long ms) {

        try {

            Thread.sleep(ms);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
}