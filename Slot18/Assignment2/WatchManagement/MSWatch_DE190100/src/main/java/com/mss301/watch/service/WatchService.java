package com.mss301.watch.service;

import com.mss301.watch.dto.WatchRequest;
import com.mss301.watch.dto.WatchResponse;
import com.mss301.watch.entity.Watch;
import com.mss301.watch.repository.WatchCategoryRepository;
import com.mss301.watch.repository.WatchRepository;
import com.mss301.watch.service.saga.SagaOutcome;
import com.mss301.watch.service.saga.WatchSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

// [TODO-6.13]
@Service
@RequiredArgsConstructor
public class WatchService {

    private final WatchRepository watchRepository;
    private final WatchCategoryRepository categoryRepository;
    private final WatchLocalService localService;
    private final WatchSagaOrchestrator sagaOrchestrator;

    // Watch mới nhất ở đầu danh sách
    public List<WatchResponse> findAll() {

        return watchRepository
                .findAllByOrderByWatchIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SagaResult<WatchResponse> create(
            WatchRequest request,
            Integer failAfterStep
    ) {

        validate(request);

        SagaOutcome outcome =
                sagaOrchestrator.createWatch(
                        WatchData.from(
                                request,
                                null
                        ),
                        failAfterStep
                );

        return new SagaResult<>(
                outcome.sagaId(),
                loadResponse(
                        outcome.watchId()
                )
        );
    }

    public SagaResult<WatchResponse> update(
            Integer id,
            WatchRequest request,
            Integer failAfterStep
    ) {

        // Snapshot cũ để RESTORE nếu Saga fail
        WatchData before =
                localService.load(id);

        validate(request);

        SagaOutcome outcome =
                sagaOrchestrator.updateWatch(
                        before,
                        WatchData.from(
                                request,
                                id
                        ),
                        failAfterStep
                );

        return new SagaResult<>(
                outcome.sagaId(),
                loadResponse(id)
        );
    }

    public SagaResult<Void> delete(
            Integer id,
            Integer failAfterStep
    ) {

        WatchData before =
                localService.load(id);

        SagaOutcome outcome =
                sagaOrchestrator.deleteWatch(
                        before,
                        failAfterStep
                );

        return new SagaResult<>(
                outcome.sagaId(),
                null
        );
    }

    // =========================
    // Business validation
    // =========================

    private void validate(
            WatchRequest request
    ) {

        if (
                !LocalDate.now()
                        .equals(
                                request.getReleaseDate()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Release date must be today's date: "
                            + LocalDate.now()
            );
        }

        if (
                !categoryRepository.existsById(
                        request.getCategoryId()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category not found with id: "
                            + request.getCategoryId()
            );
        }

        /*
         * KHÔNG check BrandID ở đây.
         *
         * Brand thuộc MSBrand.
         * Việc xác minh brand tồn tại phải diễn ra trong
         * bước ASSIGN_BRAND của Saga.
         */
    }

    private WatchResponse loadResponse(
            Integer id
    ) {

        return watchRepository
                .findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Watch not found with id: "
                                        + id
                        )
                );
    }

    private WatchResponse toResponse(
            Watch watch
    ) {

        return WatchResponse.builder()
                .watchId(
                        watch.getWatchId()
                )
                .name(
                        watch.getName()
                )
                .categoryId(
                        watch.getCategory() != null
                                ? watch.getCategory()
                                        .getCategoryId()
                                : null
                )
                .categoryName(
                        watch.getCategory() != null
                                ? watch.getCategory()
                                        .getCategoryName()
                                : null
                )
                .brandId(
                        watch.getBrandId()
                )
                .caseMaterial(
                        watch.getCaseMaterial()
                )
                .strapMaterial(
                        watch.getStrapMaterial()
                )
                .waterpMaterial(
                        watch.getWaterpMaterial()
                )
                .price(
                        watch.getPrice()
                )
                .releaseDate(
                        watch.getReleaseDate()
                )
                .stock(
                        watch.getStock()
                )
                .build();
    }
}