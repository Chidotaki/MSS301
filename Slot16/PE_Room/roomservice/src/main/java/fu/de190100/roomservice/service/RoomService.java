package fu.de190100.roomservice.service;

import fu.de190100.roomservice.dto.ApiResponseDTO;
import fu.de190100.roomservice.dto.RoomDTO;
import org.springframework.data.domain.Pageable;

public interface RoomService {

    ApiResponseDTO createRoom(RoomDTO dto);

    ApiResponseDTO updateRoom(Long roomId, RoomDTO dto);

    ApiResponseDTO getRoomById(Long roomId);

    ApiResponseDTO setRoomToMaintenance(Long roomId);

    ApiResponseDTO getRoomList(
            String roomType,
            String status,
            Pageable pageable
    );
}