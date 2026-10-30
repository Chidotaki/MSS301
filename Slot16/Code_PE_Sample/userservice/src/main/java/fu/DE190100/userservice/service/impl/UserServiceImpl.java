package fu.DE190100.userservice.service.impl;

import fu.DE190100.userservice.dto.ApiResponse;
import fu.DE190100.userservice.dto.PageDTO;
import fu.DE190100.userservice.dto.UserDTO;
import fu.DE190100.userservice.entity.Role;
import fu.DE190100.userservice.entity.User;
import fu.DE190100.userservice.entity.UserStatus;
import fu.DE190100.userservice.repository.UserRepository;
import fu.DE190100.userservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse createUser(UserDTO dto) {

        // status = 3 -> duplicated username
        if (userRepository.existsByUsername(dto.getUsername())) {
            return ApiResponse.error(
                    3,
                    "Username is duplicated"
            );
        }

        User user = new User();

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(Role.valueOf(dto.getRole()));
        user.setRatePerPost(dto.getRatePerPost());
        user.setMaxDailyPosts(dto.getMaxDailyPosts());
        user.setLevel(dto.getLevel());

        // Default ACTIVE nếu không gửi status
        user.setStatus(
                dto.getStatus() == null
                        ? UserStatus.ACTIVE
                        : UserStatus.valueOf(dto.getStatus())
        );

        User saved = userRepository.save(user);

        return ApiResponse.ok(
                "User created successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional
    public ApiResponse updateUser(
            Long userId,
            UserDTO dto
    ) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return ApiResponse.error(
                    4,
                    "User is not found"
            );
        }

        // Trùng username nhưng bỏ qua chính user hiện tại
        if (
                dto.getUsername() != null
                        && userRepository
                        .existsByUsernameAndIdNot(
                                dto.getUsername(),
                                userId
                        )
        ) {
            return ApiResponse.error(
                    3,
                    "Username is duplicated"
            );
        }

        // Partial update
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }

        if (dto.getRole() != null) {
            user.setRole(
                    Role.valueOf(dto.getRole())
            );
        }

        if (dto.getRatePerPost() != null) {
            user.setRatePerPost(
                    dto.getRatePerPost()
            );
        }

        if (dto.getMaxDailyPosts() != null) {
            user.setMaxDailyPosts(
                    dto.getMaxDailyPosts()
            );
        }

        if (dto.getLevel() != null) {
            user.setLevel(dto.getLevel());
        }

        if (dto.getStatus() != null) {
            user.setStatus(
                    UserStatus.valueOf(dto.getStatus())
            );
        }

        User saved = userRepository.save(user);

        return ApiResponse.ok(
                "User updated successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getUserById(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return ApiResponse.error(
                    4,
                    "User is not found"
            );
        }

        return ApiResponse.ok(
                "Successful",
                toDTO(user)
        );
    }

    @Override
    @Transactional
    public ApiResponse deactivateUser(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return ApiResponse.error(
                    4,
                    "User is not found"
            );
        }

        user.setStatus(UserStatus.DEACTIVATED);

        userRepository.save(user);

        return ApiResponse.ok(
                "User is set to DEACTIVATED successfully",
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getUserList(
            String role,
            String status,
            Pageable pageable
    ) {

        Page<User> page = userRepository.search(
                parseRole(role),
                parseStatus(status),
                pageable
        );

        List<UserDTO> content = page
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

        return ApiResponse.ok(
                "Successful",
                pageDTO
        );
    }

    // ---------------- HELPERS ----------------

    private UserDTO toDTO(User user) {

        UserDTO dto = new UserDTO();

        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setRatePerPost(user.getRatePerPost());
        dto.setMaxDailyPosts(user.getMaxDailyPosts());
        dto.setLevel(user.getLevel());
        dto.setStatus(user.getStatus().name());

        return dto;
    }

    private Role parseRole(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private UserStatus parseStatus(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UserStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}