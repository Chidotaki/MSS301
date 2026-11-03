package fu.de190100.roomservice.controller;

import fu.de190100.roomservice.dto.ApiResponseDTO;
import fu.de190100.roomservice.dto.RoomDTO;
import fu.de190100.roomservice.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
public class DE190100RoomController {

    private final RoomService roomService;

    public DE190100RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // 1. Create Room
    @PostMapping("/api/rooms")
    public ResponseEntity<ApiResponseDTO> createRoom(
            @Valid @RequestBody RoomDTO dto,
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
                roomService.createRoom(dto),
                HttpStatus.CREATED
        );
    }

    // 2. Update Room - partial update
    @PutMapping("/api/rooms/{roomId}")
    public ResponseEntity<ApiResponseDTO> updateRoom(
            @PathVariable Long roomId,
            @RequestBody RoomDTO dto
    ) {

        return map(
                roomService.updateRoom(roomId, dto),
                HttpStatus.OK
        );
    }

    // 3. Get Room Detail
    @GetMapping("/api/rooms/{roomId}")
    public ResponseEntity<ApiResponseDTO> getRoomById(
            @PathVariable Long roomId
    ) {

        return map(
                roomService.getRoomById(roomId),
                HttpStatus.OK
        );
    }

    // 4. Set Room to Maintenance
    @DeleteMapping("/api/rooms/{roomId}")
    public ResponseEntity<ApiResponseDTO> setRoomToMaintenance(
            @PathVariable Long roomId
    ) {

        return map(
                roomService.setRoomToMaintenance(roomId),
                HttpStatus.OK
        );
    }

    // 5. Get Room List
    @GetMapping("/api/rooms")
    public ResponseEntity<ApiResponseDTO> getRoomList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roomType,
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

        Pageable pageable = PageRequest.of(page, size);

        return map(
                roomService.getRoomList(
                        roomType,
                        status,
                        pageable
                ),
                HttpStatus.OK
        );
    }

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

            case 3 ->
                    ResponseEntity
                            .status(HttpStatus.IM_USED)
                            .body(response);

            case 4 ->
                    ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
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

    private String firstError(BindingResult bindingResult) {

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