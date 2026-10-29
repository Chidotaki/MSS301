package fu.DE190100.postservice.service.impl;

import feign.FeignException;
import fu.DE190100.postservice.client.UserClient;
import fu.DE190100.postservice.common.PostConstants;
import fu.DE190100.postservice.dto.ApiResponse;
import fu.DE190100.postservice.dto.PageDTO;
import fu.DE190100.postservice.dto.PostDTO;
import fu.DE190100.postservice.dto.PostDetailDTO;
import fu.DE190100.postservice.dto.UserDTO;
import fu.DE190100.postservice.entity.Post;
import fu.DE190100.postservice.entity.PostStatus;
import fu.DE190100.postservice.repository.PostRepository;
import fu.DE190100.postservice.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserClient userClient;
    private final JsonMapper jsonMapper;

    public PostServiceImpl(
            PostRepository postRepository,
            UserClient userClient,
            JsonMapper jsonMapper
    ) {
        this.postRepository = postRepository;
        this.userClient = userClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public ApiResponse createPost(PostDTO dto) {

        // 1. Validate date
        if (!dto.getEndDate().after(dto.getStartDate())) {
            return ApiResponse.error(
                    2,
                    "endDate must be after startDate"
            );
        }

        // 2. Fetch user from UserService via OpenFeign
        UserDTO user = fetchUser(dto.getUserId());

        if (user == null) {
            return ApiResponse.error(
                    4,
                    "User ID is not found"
            );
        }

        // 3. User must be ACTIVE
        if (!PostConstants.USER_ACTIVE.equals(user.getStatus())) {
            return ApiResponse.error(
                    5,
                    "User is not ACTIVE for posting"
            );
        }

        // 4. numberOfPosts <= maxDailyPosts
        if (dto.getNumberOfPosts() > user.getMaxDailyPosts()) {
            return ApiResponse.error(
                    2,
                    "numberOfPosts must not exceed user's maxDailyPosts"
            );
        }

        Post post = new Post();

        post.setAdvertiserName(dto.getAdvertiserName());
        post.setAdvertiserEmail(dto.getAdvertiserEmail());
        post.setAdvertiserPhone(dto.getAdvertiserPhone());
        post.setStartDate(dto.getStartDate());
        post.setEndDate(dto.getEndDate());
        post.setNumberOfPosts(dto.getNumberOfPosts());
        post.setUserId(dto.getUserId());

        // Server tự tính totalCost
        post.setTotalCost(
                computeTotal(
                        user.getRatePerPost(),
                        dto.getStartDate(),
                        dto.getEndDate()
                )
        );

        // Default status
        post.setStatus(PostStatus.SCHEDULED);

        Post saved = postRepository.save(post);

        return ApiResponse.of(
                1,
                "Post campaign created successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional
    public ApiResponse updatePost(
            Long postId,
            PostDTO dto
    ) {

        Post post = postRepository
                .findById(postId)
                .orElse(null);

        if (post == null) {
            return ApiResponse.error(
                    4,
                    "Post Id is not found"
            );
        }

        /*
         * Phải validate effective dates TRƯỚC khi sửa entity.
         */
        Date effectiveStart =
                dto.getStartDate() != null
                        ? dto.getStartDate()
                        : post.getStartDate();

        Date effectiveEnd =
                dto.getEndDate() != null
                        ? dto.getEndDate()
                        : post.getEndDate();

        if (!effectiveEnd.after(effectiveStart)) {
            return ApiResponse.error(
                    2,
                    "endDate must be after startDate"
            );
        }

        // Partial update
        if (dto.getAdvertiserName() != null) {
            post.setAdvertiserName(
                    dto.getAdvertiserName()
            );
        }

        if (dto.getAdvertiserEmail() != null) {
            post.setAdvertiserEmail(
                    dto.getAdvertiserEmail()
            );
        }

        if (dto.getAdvertiserPhone() != null) {
            post.setAdvertiserPhone(
                    dto.getAdvertiserPhone()
            );
        }

        post.setStartDate(effectiveStart);
        post.setEndDate(effectiveEnd);

        if (dto.getNumberOfPosts() != null) {
            post.setNumberOfPosts(
                    dto.getNumberOfPosts()
            );
        }

        if (dto.getStatus() != null) {
            post.setStatus(
                    PostStatus.valueOf(dto.getStatus())
            );
        }

        // Tính lại totalCost theo rate hiện tại của user
        UserDTO user = fetchUser(post.getUserId());

        if (user != null) {
            post.setTotalCost(
                    computeTotal(
                            user.getRatePerPost(),
                            post.getStartDate(),
                            post.getEndDate()
                    )
            );
        }

        Post saved = postRepository.save(post);

        return ApiResponse.ok(
                "Post campaign updated successfully",
                toDTO(saved)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getPostById(Long postId) {

        Post post = postRepository
                .findById(postId)
                .orElse(null);

        if (post == null) {
            return ApiResponse.error(
                    4,
                    "Post is not found"
            );
        }

        return ApiResponse.ok(
                "Successful",
                toDetailDTO(post)
        );
    }

    @Override
    @Transactional
    public ApiResponse cancelPost(Long postId) {

        Post post = postRepository
                .findById(postId)
                .orElse(null);

        if (post == null) {
            return ApiResponse.error(
                    4,
                    "Post is not found"
            );
        }

        post.setStatus(PostStatus.CANCELLED);

        postRepository.save(post);

        return ApiResponse.ok(
                "Post campaign is cancelled successfully",
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getPostList(
            String advertiserName,
            String status,
            Pageable pageable
    ) {

        Page<Post> page = postRepository.search(
                emptyToNull(advertiserName),
                parsePostStatus(status),
                pageable
        );

        List<PostDetailDTO> content = page
                .getContent()
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

        return ApiResponse.ok(
                "Successful",
                pageDTO
        );
    }

    // ---------------- HELPERS ----------------

    private UserDTO fetchUser(Long userId) {

        try {
            ApiResponse response =
                    userClient.getUserById(userId);

            if (response == null
                    || response.getStatus() != 1
                    || response.getData() == null) {
                return null;
            }

            return jsonMapper.convertValue(
                    response.getData(),
                    UserDTO.class
            );

        } catch (FeignException.NotFound ex) {

            // UserService trả HTTP 404
            return null;
        }
    }

    private BigDecimal computeTotal(
            BigDecimal ratePerPost,
            Date start,
            Date end
    ) {

        long days = ChronoUnit.DAYS.between(
                toLocalDate(start),
                toLocalDate(end)
        );

        if (days < 1) {
            days = 1;
        }

        return ratePerPost.multiply(
                BigDecimal.valueOf(days)
        );
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private PostDTO toDTO(Post post) {

        PostDTO dto = new PostDTO();

        dto.setPostId(post.getPostId());
        dto.setAdvertiserName(post.getAdvertiserName());
        dto.setAdvertiserEmail(post.getAdvertiserEmail());
        dto.setAdvertiserPhone(post.getAdvertiserPhone());
        dto.setStartDate(post.getStartDate());
        dto.setEndDate(post.getEndDate());
        dto.setNumberOfPosts(post.getNumberOfPosts());
        dto.setTotalCost(post.getTotalCost());
        dto.setStatus(post.getStatus().name());
        dto.setUserId(post.getUserId());

        return dto;
    }

    private PostDetailDTO toDetailDTO(Post post) {

        PostDetailDTO dto = new PostDetailDTO();

        dto.setPostId(post.getPostId());
        dto.setAdvertiserName(post.getAdvertiserName());
        dto.setAdvertiserEmail(post.getAdvertiserEmail());
        dto.setAdvertiserPhone(post.getAdvertiserPhone());
        dto.setStartDate(post.getStartDate());
        dto.setEndDate(post.getEndDate());
        dto.setNumberOfPosts(post.getNumberOfPosts());
        dto.setTotalCost(post.getTotalCost());
        dto.setStatus(post.getStatus().name());

        // Lấy full UserDTO từ UserService
        dto.setUser(
                fetchUser(post.getUserId())
        );

        return dto;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value;
    }

    private PostStatus parsePostStatus(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return PostStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}