package com.example.demo.chat.api.dto;

import com.example.demo.chat.domain.RoomMemberRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

//  Room API DTO
public class RoomDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoomRequest {
        private String roomId; // optional: 비우면 서버가 UUID 생성
    }

    @Data
    @AllArgsConstructor
    public static class RoomResponse {
        private String roomId;
        private Long hostUserId;
        private String hostLoginId;
        private LocalDateTime createdAt;
        private LocalDateTime lastActiveAt;
    }

    // 방 멤버 표시용(프론트 리스트 렌더링용): loginId + role만
    @Data
    @AllArgsConstructor
    public static class MemberSummary {
        private String loginId;
        private RoomMemberRole role;
    }



    //  Schedule Archive 목록 아이템(내가 속한 방)
    @Data
    @AllArgsConstructor
    public static class MyRoomItem {
        private String roomId;
        private String hostLoginId;
        private RoomMemberRole myRole;
        private LocalDateTime joinedAt;
        private LocalDateTime lastActiveAt;

        private boolean favorite; // [추가] 즐겨찾기 여부(별표)

        private List<MemberSummary> members; //  방 멤버 목록 (loginId, role)
    }

    //  목록 응답 래퍼(옵션)
    @Data
    @AllArgsConstructor
    public static class MyRoomsResponse {
        private List<MyRoomItem> rooms;
    }

    // [추가] 별표 토글 응답
    @Data // [추가]
    @AllArgsConstructor // [추가]
    public static class ToggleFavoriteResponse { // [추가]
        private String roomId; // [추가]
        private boolean favorite; // [추가]
    }
}
