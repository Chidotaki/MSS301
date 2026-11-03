package fu.de190100.reservationservice.controller;

import fu.de190100.reservationservice.dto.ApiResponseDTO;
import fu.de190100.reservationservice.dto.ReservationDTO;
import fu.de190100.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
public class DE190100ReservationController {

    private final ReservationService reservationService;

    public DE190100ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    // 1. Create Reservation
    @PostMapping("/api/reservations")
    public ResponseEntity<ApiResponseDTO> createReservation(
            @Valid @RequestBody ReservationDTO dto,
            BindingResult bindingResult
    ) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponseDTO.error(
                                    2,
                                    firstError(bindingResult)
                            )
                    );
        }

        return map(
                reservationService.createReservation(dto),
                HttpStatus.CREATED
        );
    }

    // 2. Update Reservation - partial update
    @PutMapping("/api/reservations/{reservationId}")
    public ResponseEntity<ApiResponseDTO> updateReservation(
            @PathVariable Long reservationId,
            @RequestBody ReservationDTO dto
    ) {

        return map(
                reservationService.updateReservation(
                        reservationId,
                        dto
                ),
                HttpStatus.OK
        );
    }

    // 3. Get Reservation Detail
    @GetMapping("/api/reservations/{reservationId}")
    public ResponseEntity<ApiResponseDTO> getReservationById(
            @PathVariable Long reservationId
    ) {

        return map(
                reservationService.getReservationById(
                        reservationId
                ),
                HttpStatus.OK
        );
    }

    // 4. Cancel Reservation
    @DeleteMapping("/api/reservations/{reservationId}")
    public ResponseEntity<ApiResponseDTO> cancelReservation(
            @PathVariable Long reservationId
    ) {

        return map(
                reservationService.cancelReservation(
                        reservationId
                ),
                HttpStatus.OK
        );
    }

    // 5. Get Reservation List
    @GetMapping("/api/reservations")
    public ResponseEntity<ApiResponseDTO> getReservationList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String status
    ) {

        if (page < 0) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponseDTO.error(
                                    2,
                                    "page must be >= 0"
                            )
                    );
        }

        if (size < 1 || size > 100) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponseDTO.error(
                                    2,
                                    "size must be between 1 and 100"
                            )
                    );
        }

        Pageable pageable =
                PageRequest.of(page, size);

        return map(
                reservationService.getReservationList(
                        guestName,
                        status,
                        pageable
                ),
                HttpStatus.OK
        );
    }

    // ========================================
    // Helpers
    // ========================================

    private ResponseEntity<ApiResponseDTO> map(
            ApiResponseDTO response,
            HttpStatus successStatus
    ) {

        return switch (response.getStatus()) {

            case 1 ->
                    ResponseEntity
                            .status(successStatus)
                            .body(response);

            case 2 ->
                    ResponseEntity
                            .status(HttpStatus.NOT_ACCEPTABLE)
                            .body(response);

            case 4 ->
                    ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(response);

            case 5 ->
                    ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(response);

            case 0 ->
                    ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(response);

            default ->
                    ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(response);
        };
    }

    private String firstError(
            BindingResult bindingResult
    ) {

        return bindingResult
                .getAllErrors()
                .isEmpty()
                ? "Validation failed"
                : bindingResult
                    .getAllErrors()
                    .get(0)
                    .getDefaultMessage();
    }
}