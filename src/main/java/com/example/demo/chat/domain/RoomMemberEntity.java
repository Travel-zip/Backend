package com.example.demo.chat.domain;

import com.example.demo.auth.domain.UserEntity; // [추가]
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// [추가] 방-유저 소속(멤버십) 테이블
@Entity
@Table(
        name = "room_members",
        indexes = {
                @Index(name = "idx_room_members_user", columnList = "user_id"),
                @Index(name = "idx_room_members_room", columnList = "room_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
        }
)
@Getter @Setter
@NoArgsConstructor
public class RoomMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  어떤 방에
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    //  어떤 유저가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    //  역할
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMemberRole role;

    //  가입 시각
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // [추가] 즐겨찾기(별표) 여부 - 유저별/방별
    @Column(name = "favorite", nullable = false) // [추가]
    private boolean favorite = false; // [추가]

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }
}
