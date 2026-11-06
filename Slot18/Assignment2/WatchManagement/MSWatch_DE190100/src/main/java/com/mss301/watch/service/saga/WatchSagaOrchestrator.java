package com.mss301.watch.service.saga;

import com.mss301.watch.client.BrandServiceClient;
import com.mss301.watch.service.WatchData;
import com.mss301.watch.service.WatchLocalService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// [TODO-6.9, TODO-6.10, TODO-6.11: Saga Orchestrator]
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchSagaOrchestrator {

        private final WatchLocalService localService;
        private final BrandServiceClient brandClient;
        private final SagaTracker tracker;
        private final SagaFaultInjector faultInjector;
        private final JsonMapper jsonMapper;

        @Value("${saga.compensation.max-attempts:3}")
        private int maxAttempts;

        @Value("${saga.compensation.backoff-ms:1000}")
        private long backoffMs;

        // =========================================================
        // CREATE
        // DBWatch CREATE_WATCH
        // DBBrand ASSIGN_BRAND
        // =========================================================

        // [TODO-6.11a: Saga CREATE_WATCH]
        public SagaOutcome createWatch(
                        WatchData data,
                        Integer failAfterStep) {

                String sagaId = tracker.start(
                                "CREATE_WATCH",
                                null,
                                toJson(
                                                Map.of(
                                                                "after",
                                                                data)))
                                .getSagaId();

                AtomicReference<Integer> watchId = new AtomicReference<>();

                List<SagaStep> steps = List.of(

                                SagaStep.of(
                                                "CREATE_WATCH",

                                                () -> {
                                                        watchId.set(
                                                                        localService.insert(data));

                                                        tracker.attachWatch(
                                                                        sagaId,
                                                                        watchId.get());
                                                },

                                                "DELETE_WATCH",

                                                () -> localService.deleteIfExists(
                                                                watchId.get())),

                                SagaStep.of(
                                                "ASSIGN_BRAND",

                                                () -> brandClient.assignWatch(
                                                                data.brandId(),
                                                                watchId.get()),

                                                "UNASSIGN_BRAND",

                                                () -> brandClient.unassignWatch(
                                                                watchId.get())));

                execute(
                                sagaId,
                                steps,
                                failAfterStep);

                return new SagaOutcome(
                                sagaId,
                                watchId.get(),
                                SagaStatus.COMPLETED);
        }

        // =========================================================
        // UPDATE
        // DBWatch UPDATE_WATCH
        // DBBrand ASSIGN_BRAND
        // =========================================================

        // [TODO-6.11b: Saga UPDATE_WATCH]
        public SagaOutcome updateWatch(
                        WatchData before,
                        WatchData after,
                        Integer failAfterStep) {

                Integer id = before.watchId();

                Map<String, Object> payload = new LinkedHashMap<>();

                payload.put(
                                "before",
                                before);

                payload.put(
                                "after",
                                after);

                String sagaId = tracker.start(
                                "UPDATE_WATCH",
                                id,
                                toJson(payload)).getSagaId();

                List<SagaStep> steps = List.of(

                                SagaStep.of(
                                                "UPDATE_WATCH",

                                                () -> localService.update(
                                                                after),

                                                "RESTORE_WATCH",

                                                () -> localService.update(
                                                                before)),

                                SagaStep.of(
                                                "ASSIGN_BRAND",

                                                () -> brandClient.assignWatch(
                                                                after.brandId(),
                                                                id),

                                                "ASSIGN_BRAND_BACK",

                                                () -> brandClient.assignWatch(
                                                                before.brandId(),
                                                                id)));

                execute(
                                sagaId,
                                steps,
                                failAfterStep);

                return new SagaOutcome(
                                sagaId,
                                id,
                                SagaStatus.COMPLETED);
        }

        // =========================================================
        // DELETE
        //
        // 1. UNASSIGN_BRAND
        // 2. DELETE_WATCH = PIVOT
        // =========================================================

        // [TODO-6.11c: Saga DELETE_WATCH]
        public SagaOutcome deleteWatch(
                        WatchData before,
                        Integer failAfterStep) {

                Integer id = before.watchId();

                String sagaId = tracker.start(
                                "DELETE_WATCH",
                                id,
                                toJson(
                                                Map.of(
                                                                "before",
                                                                before)))
                                .getSagaId();

                List<SagaStep> steps = List.of(

                                SagaStep.of(
                                                "UNASSIGN_BRAND",

                                                () -> brandClient.unassignWatch(
                                                                id),

                                                "ASSIGN_BRAND_BACK",

                                                () -> brandClient.assignWatch(
                                                                before.brandId(),
                                                                id)),

                                SagaStep.pivot(
                                                "DELETE_WATCH",

                                                () -> localService.delete(
                                                                id)));

                execute(
                                sagaId,
                                steps,
                                failAfterStep);

                return new SagaOutcome(
                                sagaId,
                                id,
                                SagaStatus.COMPLETED);
        }

        // =========================================================
        // SAGA ENGINE
        // =========================================================

        // [TODO-6.9]
        private void execute(
                        String sagaId,
                        List<SagaStep> steps,
                        Integer failAfterStep) {

                tracker.updateStatus(
                                sagaId,
                                SagaStatus.EXECUTING);

                Deque<CompletedStep> completed = new ArrayDeque<>();

                int stepNo = 0;
                String currentStep = null;

                try {

                        for (SagaStep step : steps) {

                                stepNo++;

                                currentStep = step.name();

                                tracker.currentStep(
                                                sagaId,
                                                step.name());

                                // Mỗi action là một local transaction
                                step.action().run();

                                tracker.log(
                                                sagaId,
                                                stepNo,
                                                step.name(),
                                                "EXECUTE",
                                                "SUCCESS",
                                                step.isPivot()
                                                                ? "Pivot committed - saga can only move forward now"
                                                                : "Local transaction committed");

                                log.info(
                                                "[SAGA {}] step {} {} -> SUCCESS",
                                                sagaId,
                                                stepNo,
                                                step.name());

                                // Pivot không có compensation
                                if (!step.isPivot()) {

                                        completed.push(
                                                        new CompletedStep(
                                                                        stepNo,
                                                                        step));
                                }

                                // Fault injection sau khi step commit
                                if (faultInjector.shouldFailAfter(
                                                failAfterStep,
                                                stepNo)) {

                                        if (step.isPivot()) {

                                                // Pivot đã commit -> không thể rollback nữa
                                                tracker.log(
                                                                sagaId,
                                                                stepNo,
                                                                "FAULT_INJECTION",
                                                                "FAULT",
                                                                "SKIPPED",
                                                                "Ignored: step "
                                                                                + stepNo
                                                                                + " ("
                                                                                + step.name()
                                                                                + ") is the pivot, saga cannot be rolled back");

                                        } else {

                                                currentStep = "FAULT_INJECTION after "
                                                                + step.name();

                                                throw new SimulatedSagaFailureException(
                                                                "Simulated failure after step "
                                                                                + stepNo
                                                                                + " ("
                                                                                + step.name()
                                                                                + ")");
                                        }
                                }
                        }

                        tracker.updateStatus(
                                        sagaId,
                                        SagaStatus.COMPLETED);

                        log.info(
                                        "[SAGA {}] COMPLETED",
                                        sagaId);

                } catch (RuntimeException ex) {

                        String reason = describe(ex);

                        if (ex instanceof SimulatedSagaFailureException) {

                                tracker.log(
                                                sagaId,
                                                stepNo,
                                                "FAULT_INJECTION",
                                                "FAULT",
                                                "TRIGGERED",
                                                reason);

                        } else {

                                tracker.log(
                                                sagaId,
                                                stepNo,
                                                currentStep,
                                                "EXECUTE",
                                                "FAILED",
                                                reason);
                        }

                        log.warn(
                                        "[SAGA {}] FAILED at {}: {}",
                                        sagaId,
                                        currentStep,
                                        reason);

                        tracker.fail(
                                        sagaId,
                                        reason);

                        boolean allCompensated = compensate(
                                        sagaId,
                                        completed);

                        SagaStatus finalStatus = allCompensated
                                        ? SagaStatus.COMPENSATED
                                        : SagaStatus.COMPENSATING;

                        tracker.updateStatus(
                                        sagaId,
                                        finalStatus);

                        log.warn(
                                        "[SAGA {}] finished with status {}",
                                        sagaId,
                                        finalStatus);

                        throw new SagaExecutionException(
                                        sagaId,
                                        finalStatus,
                                        currentStep,
                                        httpStatusFor(ex),
                                        reason,
                                        ex);
                }
        }

        // =========================================================
        // COMPENSATION
        // =========================================================

        // [TODO-6.10]
        private boolean compensate(
                        String sagaId,
                        Deque<CompletedStep> completed) {

                tracker.updateStatus(
                                sagaId,
                                SagaStatus.COMPENSATING);

                while (!completed.isEmpty()) {

                        CompletedStep done = completed.pop();

                        if (!compensateWithRetry(
                                        sagaId,
                                        done)) {

                                tracker.log(
                                                sagaId,
                                                done.stepNo(),
                                                "COMPENSATION_HALTED",
                                                "COMPENSATE",
                                                "FAILED",
                                                completed.size()
                                                                + " earlier step(s) still need compensation - manual intervention required");

                                log.error(
                                                "[SAGA {}] compensation halted at step {}, saga stays COMPENSATING",
                                                sagaId,
                                                done.stepNo());

                                return false;
                        }
                }

                return true;
        }

        // [TODO-6.10: retry compensation]
        private boolean compensateWithRetry(
                        String sagaId,
                        CompletedStep done) {

                SagaStep step = done.step();

                for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                        try {

                                step.compensation().run();

                                tracker.log(
                                                sagaId,
                                                done.stepNo(),
                                                step.compensationName(),
                                                "COMPENSATE",
                                                "SUCCESS",
                                                "Undo "
                                                                + step.name()
                                                                + " (attempt "
                                                                + attempt
                                                                + ")");

                                log.info(
                                                "[SAGA {}] compensate step {} {} -> SUCCESS",
                                                sagaId,
                                                done.stepNo(),
                                                step.compensationName());

                                return true;

                        } catch (RuntimeException ex) {

                                tracker.log(
                                                sagaId,
                                                done.stepNo(),
                                                step.compensationName(),
                                                "COMPENSATE",
                                                "FAILED",
                                                "Attempt "
                                                                + attempt
                                                                + "/"
                                                                + maxAttempts
                                                                + ": "
                                                                + describe(ex));

                                sleep(
                                                backoffMs);
                        }
                }

                return false;
        }

        // =========================================================
        // ERROR HANDLING
        // =========================================================

        private String describe(
                        RuntimeException ex) {

                if (ex instanceof FeignException fe) {

                        if (fe.status() == 404) {

                                return "MSBrand returned 404: "
                                                + remoteMessage(
                                                                fe,
                                                                "brand not found");
                        }

                        if (fe.status() < 0) {

                                return "MSBrand is unreachable: "
                                                + fe.getMessage();
                        }

                        return "MSBrand returned HTTP "
                                        + fe.status()
                                        + ": "
                                        + remoteMessage(
                                                        fe,
                                                        fe.getMessage());
                }

                return ex.getClass().getSimpleName()
                                + ": "
                                + ex.getMessage();
        }

        private String remoteMessage(
                        FeignException fe,
                        String fallback) {

                try {

                        String body = fe.contentUTF8();

                        if (body != null
                                        && !body.isBlank()) {

                                var node = jsonMapper
                                                .readTree(body)
                                                .get("message");

                                if (node != null
                                                && !node.isNull()) {

                                        return node.asString();
                                }
                        }

                } catch (RuntimeException ignored) {
                        // Không phải JSON -> fallback
                }

                return fallback;
        }

        private HttpStatus httpStatusFor(
                        RuntimeException ex) {

                if (ex instanceof FeignException fe) {

                        if (fe.status() == 404) {
                                return HttpStatus.BAD_REQUEST;
                        }

                        if (fe.status() < 0
                                        || fe.status() >= 500) {

                                return HttpStatus.SERVICE_UNAVAILABLE;
                        }
                }

                return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        private String toJson(
                        Object value) {

                return jsonMapper.writeValueAsString(
                                value);
        }

        private void sleep(
                        long milliseconds) {

                try {

                        Thread.sleep(
                                        milliseconds);

                } catch (InterruptedException ex) {

                        Thread.currentThread()
                                        .interrupt();
                }
        }

        private record CompletedStep(
                        int stepNo,
                        SagaStep step) {
        }
}