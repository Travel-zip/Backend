package com.example.demo.chat.application;

import com.example.demo.auth.api.ApiException;
import com.example.demo.auth.domain.UserRepository;
import com.example.demo.chat.domain.*;
import com.example.demo.map.domain.TourPlaceRepository;
import com.example.demo.schedule.domain.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository; // 방장 user 조회용
    private final RoomMemberRepository roomMemberRepository;

    private final ChatMessageRepository chatMessageRepository;
    private final TourPlaceRepository tourPlaceRepository;
    private final TravelPlanRepository travelPlanRepository;


    public RoomEntity getOrCreate(String roomId) {
        return roomRepository.findById(roomId).orElseGet(() -> {
            try {
                RoomEntity r = new RoomEntity();
                r.setRoomId(roomId);
                // createdAt/lastActiveAt은 @PrePersist에서 세팅됨
                return roomRepository.save(r);
            } catch (DataIntegrityViolationException e) {
                // 동시 생성 레이스 대비
                return roomRepository.findById(roomId).orElseThrow();
            }
        });
    }

    //  “명시적 방 생성”: 생성자는 host(방장)가 됨
    public RoomEntity createRoom(String requestedRoomId, Long hostUserId) {
        if (hostUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        var host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 사용자입니다."));

        String roomId = (requestedRoomId == null) ? "" : requestedRoomId.trim();

        // roomId를 안 주면 서버가 생성
        if (roomId.isBlank()) {
            roomId = UUID.randomUUID().toString().replace("-", ""); // 32 chars
        }

        if (roomId.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROOM_ID", "roomId는 64자 이하여야 합니다.");
        }

        var existing = roomRepository.findById(roomId);
        RoomEntity room;

        if (existing.isPresent()) {
            room = existing.get();

            // 자동 생성된 방(host null)을 “정식 생성”으로 전환 가능
            if (room.getHost() == null) {
                room.setHost(host);
                room.setLastActiveAt(LocalDateTime.now());
                room = roomRepository.save(room);
            } else {
                throw new ApiException(HttpStatus.CONFLICT, "ROOM_ALREADY_EXISTS", "이미 존재하는 방입니다.");
            }
        } else {
            room = new RoomEntity();
            room.setRoomId(roomId);
            room.setHost(host);
            room = roomRepository.save(room);
        }

        //  방장은 멤버십(HOST) 자동 보장
        upsertMember(room, host.getId(), RoomMemberRole.HOST);

        return room;
    }

    // 초대 링크로 들어온 사용자가 “방 소속”으로 등록 (Archive에 뜨게 됨)
    public void joinRoom(String roomId, Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 사용자입니다."));

        var room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."));

        // 방에 host가 없고, 아직 멤버도 없다면 “첫 가입자=HOST”로 처리(안전장치)
        if (room.getHost() == null && !roomMemberRepository.existsByRoom_RoomId(roomId)) {
            room.setHost(user);
            room = roomRepository.save(room);
            upsertMember(room, userId, RoomMemberRole.HOST);
            return;
        }

        //  이미 가입되어 있으면 그냥 통과
        if (roomMemberRepository.existsByRoom_RoomIdAndUser_Id(roomId, userId)) return;

        RoomMemberEntity rm = new RoomMemberEntity();
        rm.setRoom(room);
        rm.setUser(user);
        rm.setRole(RoomMemberRole.MEMBER);
        roomMemberRepository.save(rm);
    }

    //  Schedule Archive용: 내가 속한 방 전부 조회
    public List<RoomMemberEntity> findMyRooms(Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        return roomMemberRepository.findMyRoomsWithHost(userId);
    }

    // =========================================================
    // [추가] 즐겨찾기(별표) 토글
    // - 해당 유저가 해당 방 멤버가 아니면 403(또는 정책상 404) 처리
    // =========================================================
    @Transactional // [추가]
    public boolean toggleFavorite(String roomId, Long userId) { // [추가]
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        // 멤버십이 있어야 즐겨찾기 가능
        var rmOpt = roomMemberRepository.findByRoom_RoomIdAndUser_Id(roomId, userId); // [추가]
        if (rmOpt.isEmpty()) { // [추가]
            // 방 자체가 없으면 404, 방은 있는데 멤버 아니면 403
            if (!roomRepository.existsById(roomId)) { // [추가]
                throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."); // [추가]
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ROOM_MEMBER", "방 멤버만 즐겨찾기 할 수 있습니다."); // [추가]
        }

        RoomMemberEntity rm = rmOpt.get(); // [추가]
        rm.setFavorite(!rm.isFavorite()); // [추가] 토글
        roomMemberRepository.save(rm); // [추가]
        return rm.isFavorite(); // [추가]
    }



    // =========================================================
    // (선택지 1) 내 방 목록 응답에 멤버들을 포함시키기 위한 조회
    // - roomId 리스트를 받아 한 번에 projection 조회
    // =========================================================
    public List<RoomMemberRepository.RoomMemberRow> findMembersByRoomIds(List<String> roomIds) { // [추가]
        if (roomIds == null || roomIds.isEmpty()) return List.of(); // [추가]
        return roomMemberRepository.findMembersByRoomIds(roomIds); // [추가]
    }



    //  내부 유틸: 멤버십이 없으면 만들고, 있으면 role 갱신
    private void upsertMember(RoomEntity room, Long userId, RoomMemberRole role) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 사용자입니다."));

        var existing = roomMemberRepository.findByRoom_RoomIdAndUser_Id(room.getRoomId(), userId);
        if (existing.isPresent()) {
            RoomMemberEntity rm = existing.get();
            rm.setRole(role);
            roomMemberRepository.save(rm);
            return;
        }

        RoomMemberEntity rm = new RoomMemberEntity();
        rm.setRoom(room);
        rm.setUser(user);
        rm.setRole(role);
        roomMemberRepository.save(rm);
    }


    public void touch(String roomId) {
        RoomEntity r = getOrCreate(roomId);
        r.setLastActiveAt(LocalDateTime.now());
        roomRepository.save(r);
    }

    // 방 조회(없으면 404)
    public RoomEntity getRequired(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteRoom(String roomId, Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        // 1. 방 찾기
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."));

        // 2. 권한 체크 (방장인지 확인)
        if (room.getHost() == null || !room.getHost().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "방장만 방을 삭제할 수 있습니다.");
        }

        // 3. 엮여있는 자식 데이터들 먼저 싹 청소 (순서 무관)
        chatMessageRepository.deleteByRoom_RoomId(roomId);
        tourPlaceRepository.deleteByRoom_RoomId(roomId);
        travelPlanRepository.deleteByRoom_RoomId(roomId);
        roomMemberRepository.deleteByRoom_RoomId(roomId);

        // 4. 마지막으로 방 자체를 삭제
        roomRepository.delete(room);
    }
}
