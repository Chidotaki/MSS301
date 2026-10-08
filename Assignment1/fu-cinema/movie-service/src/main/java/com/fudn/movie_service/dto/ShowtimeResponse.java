package com.fudn.movie_service.dto;

import com.fudn.movie_service.model.CinemaRoom;
import com.fudn.movie_service.model.Movie;
import com.fudn.movie_service.model.Showtime;
import com.fudn.movie_service.model.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeResponse(
        String showtimeId,
        String movieId,
        String movieTitle,
        String roomId,
        String roomName,
        int seatRows,
        int seatsPerRow,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal ticketPrice,
        ShowtimeStatus showtimeStatus
) {
    public static ShowtimeResponse from(
            Showtime s,
            Movie movie,
            CinemaRoom room
    ) {
        return new ShowtimeResponse(
                s.getShowtimeId(),
                movie.getMovieId(),
                movie.getTitle(),
                room.getRoomId(),
                room.getRoomName(),
                room.getSeatRows(),
                room.getSeatsPerRow(),
                s.getStartTime(),
                s.getEndTime(),
                s.getTicketPrice(),
                s.getShowtimeStatus()
        );
    }
}