package com.fudn.booking_service.dto;

import java.math.BigDecimal;

public record MovieRevenueResponse(
        String movieId,
        String movieTitle,
        long ticketsSold,
        BigDecimal revenue
) {
}