package com.fudn.movie_service.service;

import com.fudn.movie_service.dto.ShowtimeRequest;
import com.fudn.movie_service.dto.ShowtimeResponse;
import com.fudn.movie_service.exception.ApiException;
import com.fudn.movie_service.model.CinemaRoom;
import com.fudn.movie_service.model.Movie;
import com.fudn.movie_service.model.MovieStatus;
import com.fudn.movie_service.model.RoomStatus;
import com.fudn.movie_service.model.Showtime;
import com.fudn.movie_service.model.ShowtimeStatus;
import com.fudn.movie_service.repository.MovieRepository;
import com.fudn.movie_service.repository.RoomRepository;
import com.fudn.movie_service.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private static final String NO_EXCLUDE = "";

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final MovieService movieService;
    private final RoomService roomService;

    public List<ShowtimeResponse> search(
            String movieId,
            LocalDate date
    ) {
        List<Showtime> showtimes =
                (movieId == null || movieId.isBlank())
                        ? showtimeRepository.findAllByOrderByStartTimeAsc()
                        : showtimeRepository.findByMovieIdOrderByStartTimeAsc(movieId);

        List<Showtime> filtered = showtimes.stream()
                .filter(showtime ->
                        date == null
                                || showtime.getStartTime()
                                .toLocalDate()
                                .equals(date)
                )
                .toList();

        return toResponses(filtered);
    }

    public ShowtimeResponse getById(String id) {
        Showtime showtime = find(id);

        return ShowtimeResponse.from(
                showtime,
                movieService.find(showtime.getMovieId()),
                roomService.find(showtime.getRoomId())
        );
    }

    public ShowtimeResponse create(ShowtimeRequest request) {
        Showtime showtime = new Showtime();

        showtime.setShowtimeStatus(
                ShowtimeStatus.SCHEDULED
        );

        return apply(
                showtime,
                request,
                NO_EXCLUDE
        );
    }

    public ShowtimeResponse update(
            String id,
            ShowtimeRequest request
    ) {
        Showtime showtime = find(id);

        if (showtime.getShowtimeStatus()
                == ShowtimeStatus.CANCELLED) {
            throw ApiException.badRequest(
                    "Cannot update a cancelled showtime"
            );
        }

        return apply(
                showtime,
                request,
                id
        );
    }

    public void cancel(String id) {
        Showtime showtime = find(id);

        showtime.setShowtimeStatus(
                ShowtimeStatus.CANCELLED
        );

        showtimeRepository.save(showtime);
    }

    private Showtime find(String id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Showtime not found with id: " + id
                        )
                );
    }

    private ShowtimeResponse apply(
            Showtime showtime,
            ShowtimeRequest request,
            String excludeId
    ) {
        Movie movie =
                movieService.find(request.movieId());

        CinemaRoom room =
                roomService.find(request.roomId());

        // BR04: movie phải còn khả dụng
        if (movie.getMovieStatus()
                == MovieStatus.ENDED) {
            throw ApiException.badRequest(
                    "Movie '" + movie.getTitle()
                            + "' has ENDED and cannot be scheduled"
            );
        }

        // BR04: phòng phải ACTIVE
        if (room.getRoomStatus()
                != RoomStatus.ACTIVE) {
            throw ApiException.badRequest(
                    "Room '" + room.getRoomName()
                            + "' is not ACTIVE"
            );
        }

        // BR04: giờ chiếu phải ở tương lai
        if (!request.startTime()
                .isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest(
                    "Start time must be in the future"
            );
        }

        // BR04: server tự tính giờ kết thúc
        LocalDateTime endTime =
                request.startTime()
                        .plusMinutes(
                                movie.getDurationMinutes()
                        );

        // BR05: kiểm tra trùng lịch cùng phòng
        long overlaps =
                showtimeRepository
                        .countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(
                                room.getRoomId(),
                                ShowtimeStatus.SCHEDULED,
                                endTime,
                                request.startTime(),
                                excludeId
                        );

        if (overlaps > 0) {
            throw ApiException.conflict(
                    "Room '" + room.getRoomName()
                            + "' already has a showtime between "
                            + request.startTime()
                            + " and "
                            + endTime
            );
        }

        showtime.setMovieId(
                movie.getMovieId()
        );
        showtime.setRoomId(
                room.getRoomId()
        );
        showtime.setStartTime(
                request.startTime()
        );
        showtime.setEndTime(
                endTime
        );
        showtime.setTicketPrice(
                request.ticketPrice()
        );

        Showtime saved =
                showtimeRepository.save(showtime);

        return ShowtimeResponse.from(
                saved,
                movie,
                room
        );
    }

    private List<ShowtimeResponse> toResponses(
            List<Showtime> showtimes
    ) {
        Map<String, Movie> movies =
                movieRepository
                        .findAllById(
                                showtimes.stream()
                                        .map(Showtime::getMovieId)
                                        .distinct()
                                        .toList()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Movie::getMovieId,
                                        Function.identity()
                                )
                        );

        Map<String, CinemaRoom> rooms =
                roomRepository
                        .findAllById(
                                showtimes.stream()
                                        .map(Showtime::getRoomId)
                                        .distinct()
                                        .toList()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        CinemaRoom::getRoomId,
                                        Function.identity()
                                )
                        );

        return showtimes.stream()
                .map(showtime ->
                        ShowtimeResponse.from(
                                showtime,
                                movies.get(
                                        showtime.getMovieId()
                                ),
                                rooms.get(
                                        showtime.getRoomId()
                                )
                        )
                )
                .toList();
    }
}