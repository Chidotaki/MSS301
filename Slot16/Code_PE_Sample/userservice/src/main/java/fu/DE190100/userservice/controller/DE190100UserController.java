package fu.DE190100.userservice.controller;

import fu.DE190100.userservice.dto.ApiResponse;
import fu.DE190100.userservice.dto.UserDTO;
import fu.DE190100.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class DE190100UserController {

    private final UserService userService;

    public DE190100UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Create User -> 201/1, 406/2, 226/3, 500/0
    @PostMapping
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody UserDTO userDTO,
            BindingResult br
    ) {
        if (br.hasErrors()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(
                            2,
                            firstError(br)
                    ));
        }

        return map(
                userService.createUser(userDTO),
                HttpStatus.CREATED
        );
    }

    // 2. Update User -> 200/1, 406/2, 226/3, 404/4, 500/0
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserDTO userDTO,
            BindingResult br
    ) {
        if (br.hasErrors()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(
                            2,
                            firstError(br)
                    ));
        }

        return map(
                userService.updateUser(userId, userDTO),
                HttpStatus.OK
        );
    }

    // 3. Get User Detail -> 200/1, 404/4, 500/0
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getDetail(
            @PathVariable Long userId
    ) {
        return map(
                userService.getUserById(userId),
                HttpStatus.OK
        );
    }

    // 4. Set to DEACTIVATED -> 200/1, 404/4, 500/0
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deactivate(
            @PathVariable Long userId
    ) {
        return map(
                userService.deactivateUser(userId),
                HttpStatus.OK
        );
    }

    // 5. Get User List -> 200/1, 406/2, 500/0
    @GetMapping
    public ResponseEntity<ApiResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {

        if (size < 1 || size > 100) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(
                            2,
                            "size must be between 1 and 100"
                    ));
        }

        Pageable pageable = PageRequest.of(page, size);

        return map(
                userService.getUserList(
                        role,
                        status,
                        pageable
                ),
                HttpStatus.OK
        );
    }

    // ---------- Helpers ----------

    private ResponseEntity<ApiResponse> map(
            ApiResponse response,
            HttpStatus okStatus
    ) {

        int status = response.getStatus();

        HttpStatus httpStatus;

        switch (status) {
            case 1:
                httpStatus = okStatus;
                break;

            case 2:
                httpStatus = HttpStatus.NOT_ACCEPTABLE; // 406
                break;

            case 3:
                httpStatus = HttpStatus.IM_USED; // 226
                break;

            case 4:
                httpStatus = HttpStatus.NOT_FOUND; // 404
                break;

            default:
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // 500
                break;
        }

        return ResponseEntity
                .status(httpStatus)
                .body(response);
    }

    private String firstError(BindingResult br) {
        return br.getAllErrors().isEmpty()
                ? "Validation failed"
                : br.getAllErrors()
                    .get(0)
                    .getDefaultMessage();
    }
}