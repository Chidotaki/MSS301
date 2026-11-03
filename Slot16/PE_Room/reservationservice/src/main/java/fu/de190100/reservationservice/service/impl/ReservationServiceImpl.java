package fu.de190100.reservationservice.service.impl;

import feign.FeignException;
import fu.de190100.reservationservice.client.RoomClient;
import fu.de190100.reservationservice.dto.*;
import fu.de190100.reservationservice.entity.Reservation;
import fu.de190100.reservationservice.entity.ReservationStatus;
import fu.de190100.reservationservice.repository.ReservationRepository;
import fu.de190100.reservationservice.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomClient roomClient;
    private final JsonMapper jsonMapper;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            RoomClient roomClient,
            JsonMapper jsonMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.roomClient = roomClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public ApiResponseDTO createReservation(ReservationDTO dto) {

        // 1. checkOutDate phải sau checkInDate
        if (!isAfter(
                dto.getCheckOutDate(),
                dto.getCheckInDate()
        )) {
            return ApiResponseDTO.error(
                    2,
                    "checkOutDate must be after checkInDate"
            );
        }

        // 2. Lấy room từ RoomService
        RoomDTO room = fetchRoom(dto.getRoomId());

        if (room == null) {
            return ApiResponseDTO.error(
                    4,
                    "Room ID is not found"
            );
        }

        // 3. Room phải AVAILABLE
        if (!"AVAILABLE".equals(room.getStatus())) {
            return ApiResponseDTO.error(
                    5,
                    "Room is not AVAILABLE for reservation"
            );
        }

        // 4. Số khách không vượt capacity
        if (dto.getNumberOfGuests() > room.getCapacity()) {
            return ApiResponseDTO.error(
                    2,
                    "numberOfGuests must not exceed room capacity"
            );
        }

        Reservation reservation = new Reservation();

        reservation.setGuestName(dto.getGuestName());
        reservation.setGuestEmail(dto.getGuestEmail());
        reservation.setGuestPhone(dto.getGuestPhone());

        reservation.setCheckInDate(dto.getCheckInDate());
        reservation.setCheckOutDate(dto.getCheckOutDate());

        reservation.setNumberOfGuests(
                dto.getNumberOfGuests()
        );

        reservation.setRoomId(dto.getRoomId());

        reservation.setTotalAmount(
                computeTotalAmount(
                        room.getPricePerNight(),
                        dto.getCheckInDate(),
                        dto.getCheckOutDate()
                )
        );

        // Default status theo đề
        reservation.setStatus(
                ReservationStatus.CONFIRMED
        );

        Reservation saved =
                reservationRepository.save(reservation);

        return ApiResponseDTO.of(
                1,
                "Reservation created successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional
    public ApiResponseDTO updateReservation(
            Long reservationId,
            ReservationDTO dto
    ) {

        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElse(null);

        if (reservation == null) {
            return ApiResponseDTO.error(
                    4,
                    "Reservation is not found"
            );
        }

        String validationError =
                validatePartialUpdate(dto);

        if (validationError != null) {
            return ApiResponseDTO.error(
                    2,
                    validationError
            );
        }

        /*
         * Phải tính effective dates trước khi sửa entity.
         * Tránh sửa entity managed rồi return sớm.
         */
        Date effectiveCheckIn =
                dto.getCheckInDate() != null
                        ? dto.getCheckInDate()
                        : reservation.getCheckInDate();

        Date effectiveCheckOut =
                dto.getCheckOutDate() != null
                        ? dto.getCheckOutDate()
                        : reservation.getCheckOutDate();

        if (!isAfter(
                effectiveCheckOut,
                effectiveCheckIn
        )) {
            return ApiResponseDTO.error(
                    2,
                    "checkOutDate must be after checkInDate"
            );
        }

        /*
         * Room hiện tại dùng để:
         * - kiểm tra capacity nếu numberOfGuests thay đổi
         * - tính lại totalAmount nếu ngày thay đổi
         *
         * Giống pattern PostService trước:
         * update không đổi roomId.
         */
        RoomDTO room =
                fetchRoom(reservation.getRoomId());

        Integer effectiveGuests =
                dto.getNumberOfGuests() != null
                        ? dto.getNumberOfGuests()
                        : reservation.getNumberOfGuests();

        if (room != null
                && effectiveGuests > room.getCapacity()) {

            return ApiResponseDTO.error(
                    2,
                    "numberOfGuests must not exceed room capacity"
            );
        }

        // Partial update

        if (dto.getGuestName() != null) {
            reservation.setGuestName(
                    dto.getGuestName()
            );
        }

        if (dto.getGuestEmail() != null) {
            reservation.setGuestEmail(
                    dto.getGuestEmail()
            );
        }

        if (dto.getGuestPhone() != null) {
            reservation.setGuestPhone(
                    dto.getGuestPhone()
            );
        }

        reservation.setCheckInDate(
                effectiveCheckIn
        );

        reservation.setCheckOutDate(
                effectiveCheckOut
        );

        if (dto.getNumberOfGuests() != null) {
            reservation.setNumberOfGuests(
                    dto.getNumberOfGuests()
            );
        }

        if (dto.getStatus() != null) {
            reservation.setStatus(
                    dto.getStatus()
            );
        }

        // Tính lại tiền theo giá hiện tại của room
        if (room != null) {
            reservation.setTotalAmount(
                    computeTotalAmount(
                            room.getPricePerNight(),
                            effectiveCheckIn,
                            effectiveCheckOut
                    )
            );
        }

        Reservation saved =
                reservationRepository.save(reservation);

        return ApiResponseDTO.ok(
                "Reservation updated successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservationById(
            Long reservationId
    ) {

        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElse(null);

        if (reservation == null) {
            return ApiResponseDTO.error(
                    4,
                    "Reservation is not found"
            );
        }

        return ApiResponseDTO.ok(
                "Successful",
                toDetailDTO(reservation)
        );
    }

    @Override
    @Transactional
    public ApiResponseDTO cancelReservation(
            Long reservationId
    ) {

        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElse(null);

        if (reservation == null) {
            return ApiResponseDTO.error(
                    4,
                    "Reservation is not found"
            );
        }

        reservation.setStatus(
                ReservationStatus.CANCELLED
        );

        reservationRepository.save(reservation);

        return ApiResponseDTO.ok(
                "Reservation cancelled successfully",
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservationList(
            String guestName,
            String status,
            Pageable pageable
    ) {

        ReservationStatus parsedStatus =
                parseReservationStatus(status);

        if (status != null
                && !status.isBlank()
                && parsedStatus == null) {

            return ApiResponseDTO.error(
                    2,
                    "Invalid reservation status"
            );
        }

        Page<Reservation> page =
                reservationRepository.search(
                        emptyToNull(guestName),
                        parsedStatus,
                        pageable
                );

        List<ReservationDetailDTO> content =
                page.getContent()
                        .stream()
                        .map(this::toDetailDTO)
                        .toList();

        PageDTO pageDTO = new PageDTO(
                page.getSize(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast(),
                content
        );

        return ApiResponseDTO.ok(
                "Successful",
                pageDTO
        );
    }

    // ========================================
    // Helpers
    // ========================================

    private RoomDTO fetchRoom(Long roomId) {

        try {

            ApiResponseDTO response =
                    roomClient.getRoomById(roomId);

            if (response == null
                    || response.getStatus() != 1
                    || response.getData() == null) {

                return null;
            }

            return jsonMapper.convertValue(
                    response.getData(),
                    RoomDTO.class
            );

        } catch (FeignException.NotFound ex) {

            // RoomService HTTP 404
            return null;
        }
    }

    private BigDecimal computeTotalAmount(
            BigDecimal pricePerNight,
            Date checkInDate,
            Date checkOutDate
    ) {

        long nights = ChronoUnit.DAYS.between(
                toLocalDate(checkInDate),
                toLocalDate(checkOutDate)
        );

        if (nights < 1) {
            nights = 1;
        }

        return pricePerNight.multiply(
                BigDecimal.valueOf(nights)
        );
    }

    private boolean isAfter(
            Date later,
            Date earlier
    ) {

        return toLocalDate(later)
                .isAfter(toLocalDate(earlier));
    }

    /*
     * Xử lý cả java.util.Date và java.sql.Date.
     * Tránh lỗi java.sql.Date.toInstant().
     */
    private LocalDate toLocalDate(Date date) {

        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return date
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private ReservationDTO toDTO(
            Reservation reservation
    ) {

        ReservationDTO dto =
                new ReservationDTO();

        dto.setReservationId(
                reservation.getReservationId()
        );

        dto.setGuestName(
                reservation.getGuestName()
        );

        dto.setGuestEmail(
                reservation.getGuestEmail()
        );

        dto.setGuestPhone(
                reservation.getGuestPhone()
        );

        dto.setCheckInDate(
                reservation.getCheckInDate()
        );

        dto.setCheckOutDate(
                reservation.getCheckOutDate()
        );

        dto.setNumberOfGuests(
                reservation.getNumberOfGuests()
        );

        dto.setTotalAmount(
                reservation.getTotalAmount()
        );

        dto.setStatus(
                reservation.getStatus()
        );

        dto.setRoomId(
                reservation.getRoomId()
        );

        return dto;
    }

    private ReservationDetailDTO toDetailDTO(
            Reservation reservation
    ) {

        ReservationDetailDTO dto =
                new ReservationDetailDTO();

        dto.setReservationId(
                reservation.getReservationId()
        );

        dto.setGuestName(
                reservation.getGuestName()
        );

        dto.setGuestEmail(
                reservation.getGuestEmail()
        );

        dto.setGuestPhone(
                reservation.getGuestPhone()
        );

        dto.setCheckInDate(
                reservation.getCheckInDate()
        );

        dto.setCheckOutDate(
                reservation.getCheckOutDate()
        );

        dto.setNumberOfGuests(
                reservation.getNumberOfGuests()
        );

        dto.setTotalAmount(
                reservation.getTotalAmount()
        );

        dto.setStatus(
                reservation.getStatus()
        );

        // Full room object từ RoomService
        dto.setRoom(
                fetchRoom(reservation.getRoomId())
        );

        return dto;
    }

    private ReservationStatus parseReservationStatus(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ReservationStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String emptyToNull(String value) {

        return value == null || value.isBlank()
                ? null
                : value;
    }

    private String validatePartialUpdate(
            ReservationDTO dto
    ) {

        if (dto.getGuestName() != null) {

            if (dto.getGuestName().isBlank()) {
                return "guestName must not be blank";
            }

            if (dto.getGuestName().length() > 100) {
                return "guestName must not exceed 100 characters";
            }
        }

        if (dto.getGuestEmail() != null) {

            if (dto.getGuestEmail().isBlank()) {
                return "guestEmail must not be blank";
            }

            if (dto.getGuestEmail().length() > 100) {
                return "guestEmail must not exceed 100 characters";
            }

            if (!dto.getGuestEmail().matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
            )) {
                return "guestEmail is invalid";
            }
        }

        if (dto.getGuestPhone() != null) {

            if (dto.getGuestPhone().isBlank()) {
                return "guestPhone must not be blank";
            }

            if (dto.getGuestPhone().length() > 20) {
                return "guestPhone must not exceed 20 characters";
            }
        }

        if (dto.getNumberOfGuests() != null
                && (dto.getNumberOfGuests() < 1
                || dto.getNumberOfGuests() > 10)) {

            return "numberOfGuests must be between 1 and 10";
        }

        return null;
    }
}