package com.fudn.booking_service.repository;

import com.fudn.booking_service.model.BookingDetail;
import com.fudn.booking_service.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingDetailRepository
        extends JpaRepository<BookingDetail, Long> {

    @Query("""
            select d.seatCode from BookingDetail d
            where d.showtimeId = :showtimeId
              and d.booking.bookingStatus = :status
            """)
    List<String> findSeatCodesByShowtime(
            @Param("showtimeId") String showtimeId,
            @Param("status") BookingStatus status
    );
}