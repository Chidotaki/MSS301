package com.mss301.brand.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "BrandWatches",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_BrandWatches_WatchID",
                columnNames = "WatchID"
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "WatchID", nullable = false)
    private Integer watchId;

    @Column(name = "BrandID", nullable = false)
    private Integer brandId;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}