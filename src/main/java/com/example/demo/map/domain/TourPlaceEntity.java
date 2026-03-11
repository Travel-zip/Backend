package com.example.demo.map.domain;

import com.example.demo.chat.domain.RoomEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tour_places",
        indexes = {
                @Index(name = "idx_tour_room_time", columnList = "room_id, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_title_lat_lng",
                        columnNames = {"room_id", "title", "lat", "lng"}
                )
        }
)
@Getter @Setter
@NoArgsConstructor
public class TourPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }


}
