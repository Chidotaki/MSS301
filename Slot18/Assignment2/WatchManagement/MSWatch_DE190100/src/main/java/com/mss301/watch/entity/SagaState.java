package com.mss301.watch.entity;

import com.mss301.watch.service.saga.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SagaStates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "SagaId",
            nullable = false,
            unique = true,
            length = 36
    )
    private String sagaId;

    @Column(
            name = "SagaType",
            length = 20
    )
    private String sagaType;

    @Column(name = "WatchID")
    private Integer watchId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "Status",
            length = 20
    )
    private SagaStatus status;

    @Column(
            name = "CurrentStep",
            length = 50
    )
    private String currentStep;

    @Column(
            name = "Payload",
            columnDefinition = "NVARCHAR(MAX)"
    )
    private String payload;

    @Column(
            name = "FailureReason",
            length = 1000
    )
    private String failureReason;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}