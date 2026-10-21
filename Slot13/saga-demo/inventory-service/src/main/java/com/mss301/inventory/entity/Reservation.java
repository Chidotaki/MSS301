package com.mss301.inventory.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reservation")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    private Long orderId;

    private String productId;

    private int quantity;

    private ReservationStatus status;
}