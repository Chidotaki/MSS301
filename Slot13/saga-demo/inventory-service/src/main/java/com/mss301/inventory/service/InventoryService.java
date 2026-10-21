package com.mss301.inventory.service;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.entity.Product;
import com.mss301.inventory.entity.Reservation;
import com.mss301.inventory.entity.ReservationStatus;
import com.mss301.inventory.repository.ProductRepository;
import com.mss301.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation reserve(ReserveRequest req) {

        var existing = reservationRepository.findById(req.orderId());

        if (existing.isPresent()) {
            log.info("Order {} đã được reserve trước đó -> bỏ qua", req.orderId());
            return existing.get();
        }

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm " + req.productId()
                ));

        if (product.getStock() < req.quantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không đủ hàng: còn " + product.getStock()
                            + ", cần " + req.quantity()
            );
        }

        product.setStock(product.getStock() - req.quantity());
        productRepository.save(product);

        Reservation reservation = new Reservation();
        reservation.setOrderId(req.orderId());
        reservation.setProductId(req.productId());
        reservation.setQuantity(req.quantity());
        reservation.setStatus(ReservationStatus.RESERVED);

        log.info(
                "RESERVE order={} product={} qty={} -> stock còn {}",
                req.orderId(),
                req.productId(),
                req.quantity(),
                product.getStock()
        );

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void release(Long orderId) {

        var opt = reservationRepository.findById(orderId);

        if (opt.isEmpty()
                || opt.get().getStatus() == ReservationStatus.RELEASED) {

            log.info(
                    "RELEASE order={} -> không có gì để hoàn tác",
                    orderId
            );

            return;
        }

        Reservation reservation = opt.get();

        Product product = productRepository
                .findById(reservation.getProductId())
                .orElseThrow();

        product.setStock(
                product.getStock() + reservation.getQuantity()
        );

        reservation.setStatus(ReservationStatus.RELEASED);

        productRepository.save(product);
        reservationRepository.save(reservation);

        log.info(
                "RELEASE order={} -> trả lại {} {}, stock = {}",
                orderId,
                reservation.getQuantity(),
                product.getId(),
                product.getStock()
        );
    }
}