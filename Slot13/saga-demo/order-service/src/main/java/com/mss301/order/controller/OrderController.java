package com.mss301.order.controller;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import com.mss301.order.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;
    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;

    @PostMapping
    public Order placeOrder(
            @RequestBody CreateOrderRequest req
    ) {
        return orchestrator.placeOrder(req);
    }

    @GetMapping
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}/saga-logs")
    public List<SagaLog> sagaLogs(
            @PathVariable Long id
    ) {
        return sagaLogRepository
                .findByOrderIdOrderByIdAsc(id);
    }
}