package fu.de190100.roomservice.service.impl;

import fu.de190100.roomservice.dto.ApiResponseDTO;
import fu.de190100.roomservice.dto.PageDTO;
import fu.de190100.roomservice.dto.RoomDTO;
import fu.de190100.roomservice.entity.Room;
import fu.de190100.roomservice.entity.RoomStatus;
import fu.de190100.roomservice.entity.RoomType;
import fu.de190100.roomservice.repository.RoomRepository;
import fu.de190100.roomservice.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public ApiResponseDTO createRoom(RoomDTO dto) {

        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            return ApiResponseDTO.error(
                    3,
                    "Room number is duplicated"
            );
        }

        Room room = new Room();

        room.setRoomNumber(dto.getRoomNumber());
        room.setRoomType(dto.getRoomType());
        room.setPricePerNight(dto.getPricePerNight());
        room.setCapacity(dto.getCapacity());
        room.setFloor(dto.getFloor());

        // Đề yêu cầu room mới mặc định AVAILABLE
        room.setStatus(RoomStatus.AVAILABLE);

        Room saved = roomRepository.save(room);

        return ApiResponseDTO.of(
                1,
                "Room created successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional
    public ApiResponseDTO updateRoom(
            Long roomId,
            RoomDTO dto
    ) {

        Room room = roomRepository
                .findById(roomId)
                .orElse(null);

        if (room == null) {
            return ApiResponseDTO.error(
                    4,
                    "Room is not found"
            );
        }

        // Validate các field được gửi lên trong partial update
        String validationError = validatePartialUpdate(dto);

        if (validationError != null) {
            return ApiResponseDTO.error(
                    2,
                    validationError
            );
        }

        if (dto.getRoomNumber() != null) {

            if (roomRepository.existsByRoomNumberAndRoomIdNot(
                    dto.getRoomNumber(),
                    roomId
            )) {
                return ApiResponseDTO.error(
                        3,
                        "Room number is duplicated"
                );
            }

            room.setRoomNumber(dto.getRoomNumber());
        }

        if (dto.getRoomType() != null) {
            room.setRoomType(dto.getRoomType());
        }

        if (dto.getPricePerNight() != null) {
            room.setPricePerNight(dto.getPricePerNight());
        }

        if (dto.getCapacity() != null) {
            room.setCapacity(dto.getCapacity());
        }

        if (dto.getFloor() != null) {
            room.setFloor(dto.getFloor());
        }

        if (dto.getStatus() != null) {
            room.setStatus(dto.getStatus());
        }

        Room saved = roomRepository.save(room);

        return ApiResponseDTO.ok(
                "Room updated successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRoomById(Long roomId) {

        Room room = roomRepository
                .findById(roomId)
                .orElse(null);

        if (room == null) {
            return ApiResponseDTO.error(
                    4,
                    "Room is not found"
            );
        }

        return ApiResponseDTO.ok(
                "Successful",
                toDTO(room)
        );
    }

    @Override
    @Transactional
    public ApiResponseDTO setRoomToMaintenance(Long roomId) {

        Room room = roomRepository
                .findById(roomId)
                .orElse(null);

        if (room == null) {
            return ApiResponseDTO.error(
                    4,
                    "Room is not found"
            );
        }

        room.setStatus(RoomStatus.MAINTENANCE);

        roomRepository.save(room);

        return ApiResponseDTO.ok(
                "Room set to MAINTENANCE successfully",
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRoomList(
            String roomType,
            String status,
            Pageable pageable
    ) {

        RoomType parsedRoomType = parseRoomType(roomType);

        if (roomType != null
                && !roomType.isBlank()
                && parsedRoomType == null) {

            return ApiResponseDTO.error(
                    2,
                    "Invalid roomType"
            );
        }

        RoomStatus parsedStatus = parseRoomStatus(status);

        if (status != null
                && !status.isBlank()
                && parsedStatus == null) {

            return ApiResponseDTO.error(
                    2,
                    "Invalid status"
            );
        }

        Page<Room> page = roomRepository.search(
                parsedRoomType,
                parsedStatus,
                pageable
        );

        List<RoomDTO> content = page
                .getContent()
                .stream()
                .map(this::toDTO)
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

    // ---------------- HELPERS ----------------

    private RoomDTO toDTO(Room room) {

        RoomDTO dto = new RoomDTO();

        dto.setRoomId(room.getRoomId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomType(room.getRoomType());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setCapacity(room.getCapacity());
        dto.setFloor(room.getFloor());
        dto.setStatus(room.getStatus());

        return dto;
    }

    private RoomType parseRoomType(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoomType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RoomStatus parseRoomStatus(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoomStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String validatePartialUpdate(RoomDTO dto) {

        if (dto.getRoomNumber() != null) {

            if (dto.getRoomNumber().isBlank()) {
                return "roomNumber must not be blank";
            }

            if (dto.getRoomNumber().length() > 20) {
                return "roomNumber must not exceed 20 characters";
            }
        }

        if (dto.getPricePerNight() != null
                && dto.getPricePerNight().signum() <= 0) {
            return "pricePerNight must be > 0";
        }

        if (dto.getCapacity() != null
                && (dto.getCapacity() < 1
                || dto.getCapacity() > 10)) {
            return "capacity must be between 1 and 10";
        }

        if (dto.getFloor() != null
                && dto.getFloor() < 1) {
            return "floor must be >= 1";
        }

        return null;
    }
}