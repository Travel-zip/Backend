package com.example.demo.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 멤버십 조회용 레포
public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, Long> {

    boolean existsByRoom_RoomIdAndUser_Id(String roomId, Long userId);

    Optional<RoomMemberEntity> findByRoom_RoomIdAndUser_Id(String roomId, Long userId);

    boolean existsByRoom_RoomId(String roomId); //  방에 멤버가 하나라도 있는지

    @Query("""
           select rm
           from RoomMemberEntity rm
           join fetch rm.room r
           left join fetch r.host h
           where rm.user.id = :userId
           order by rm.favorite desc, rm.joinedAt desc
           """)
    List<RoomMemberEntity> findMyRoomsWithHost(@Param("userId") Long userId); //  Archive 목록용

    // =========================================================
    // [추가] 방 리스트 화면에서 멤버를 바로 보여주기 위한 조회
    // - roomId 목록을 받아서 (roomId, loginId, role)만 projection으로 반환
    // - N+1 방지: 한 번에 가져와서 roomId로 그룹핑
    // =========================================================

    // [추가] Projection 인터페이스
    interface RoomMemberRow { // [추가]
        String getRoomId(); // [추가]
        String getLoginId(); // [추가]
        RoomMemberRole getRole(); // [추가]
    }

    @Query("""
           select r.roomId as roomId,
                  u.loginId as loginId,
                  rm.role as role
           from RoomMemberEntity rm
           join rm.room r
           join rm.user u
           where r.roomId in :roomIds
           order by r.roomId asc,
                    case when rm.role = com.example.demo.chat.domain.RoomMemberRole.HOST then 0 else 1 end,
                    rm.joinedAt asc
           """) // [추가]
    List<RoomMemberRow> findMembersByRoomIds(@Param("roomIds") List<String> roomIds); // [추가]
}
