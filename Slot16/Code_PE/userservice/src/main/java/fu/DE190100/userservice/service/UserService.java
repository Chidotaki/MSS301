package fu.DE190100.userservice.service;

import fu.DE190100.userservice.dto.ApiResponse;
import fu.DE190100.userservice.dto.UserDTO;
import org.springframework.data.domain.Pageable;

public interface UserService {

    ApiResponse createUser(UserDTO dto);

    ApiResponse updateUser(Long userId, UserDTO dto);

    ApiResponse getUserById(Long userId);

    ApiResponse deactivateUser(Long userId);

    ApiResponse getUserList(
            String role,
            String status,
            Pageable pageable
    );
}