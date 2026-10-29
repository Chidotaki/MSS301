package fu.DE190100.postservice.controller;

import fu.DE190100.postservice.dto.ApiResponse;
import fu.DE190100.postservice.dto.PostDTO;
import fu.DE190100.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
public class DE190100PostController {

    private final PostService postService;

    public DE190100PostController(PostService postService) {
        this.postService = postService;
    }

    // 1. Create Post -> 201/1, 406/2, 404/4, 400/5, 500/0
    @PostMapping("/api/posts")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody PostDTO dto,
            BindingResult br
    ) {

        if (br.hasErrors()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponse.error(
                                    2,
                                    firstError(br)
                            )
                    );
        }

        return map(
                postService.createPost(dto),
                HttpStatus.CREATED
        );
    }

    // 2. Update Post -> 200/1, 406/2, 404/4, 500/0
    @PutMapping("/api/posts/{postId}")
    public ResponseEntity<ApiResponse> update(
            @PathVariable Long postId,
            @RequestBody PostDTO dto,
            BindingResult br
    ) {

        if (br.hasErrors()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponse.error(
                                    2,
                                    firstError(br)
                            )
                    );
        }

        return map(
                postService.updatePost(postId, dto),
                HttpStatus.OK
        );
    }

    // 3. Get Post Detail -> 200/1, 404/4, 500/0
    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<ApiResponse> getById(
            @PathVariable Long postId
    ) {

        return map(
                postService.getPostById(postId),
                HttpStatus.OK
        );
    }

    // 4. Cancel Post -> 200/1, 404/4, 500/0
    @DeleteMapping("/api/posts/{postId}")
    public ResponseEntity<ApiResponse> cancel(
            @PathVariable Long postId
    ) {

        return map(
                postService.cancelPost(postId),
                HttpStatus.OK
        );
    }

    // 5. Get Post List -> 200/1, 406/2, 500/0
    @GetMapping("/api/posts")
    public ResponseEntity<ApiResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String advertiserName,
            @RequestParam(required = false) String status
    ) {

        if (size < 1 || size > 100) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .body(
                            ApiResponse.error(
                                    2,
                                    "size must be between 1 and 100"
                            )
                    );
        }

        Pageable pageable = PageRequest.of(page, size);

        return map(
                postService.getPostList(
                        advertiserName,
                        status,
                        pageable
                ),
                HttpStatus.OK
        );
    }

    // Map service status -> HTTP status
    private ResponseEntity<ApiResponse> map(
            ApiResponse response,
            HttpStatus okStatus
    ) {

        return switch (response.getStatus()) {

            case 1 ->
                    ResponseEntity
                            .status(okStatus)
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
                            .status(HttpStatus.NOT_ACCEPTABLE)
                            .body(response);
        };
    }

    private String firstError(BindingResult br) {
        return br.getAllErrors().isEmpty()
                ? "Validation failed"
                : br.getAllErrors()
                    .get(0)
                    .getDefaultMessage();
    }
}