package com.example.demo.chat.api;

import com.example.demo.auth.api.ApiException;
import com.example.demo.auth.application.AuthRequestAttr;
import com.example.demo.chat.application.RoomService;
import com.example.demo.chat.domain.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.demo.chat.api.dto.RoomDtos.*;

// 방 생성/조회 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomApiController {

    private final RoomService roomService;

    // 방 생성: 생성한 사람이 방장(host)
    @PostMapping
    public RoomResponse create(@RequestBody CreateRoomRequest req, HttpServletRequest request) {

        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        var room = roomService.createRoom(req == null ? null : req.getRoomId(), userId);

        Long hostUserId = (room.getHost() == null) ? null : room.getHost().getId();
        String hostLoginId = (room.getHost() == null) ? null : room.getHost().getLoginId();

        return new RoomResponse(
                room.getRoomId(),
                hostUserId,
                hostLoginId,
                room.getCreatedAt(),
                room.getLastActiveAt()
        );
    }

    // 방 조회(누가 방장인지 확인용)
    @GetMapping("/{roomId}")
    public RoomResponse get(@PathVariable String roomId) {

        var room = roomService.getRequired(roomId);

        Long hostUserId = (room.getHost() == null) ? null : room.getHost().getId();
        String hostLoginId = (room.getHost() == null) ? null : room.getHost().getLoginId();

        return new RoomResponse(
                room.getRoomId(),
                hostUserId,
                hostLoginId,
                room.getCreatedAt(),
                room.getLastActiveAt()
        );
    }

    //  방 참가(초대 링크 클릭 후 프론트가 호출해주면, 이제 “내가 속한 방”에 포함됨)
    @PostMapping("/{roomId}/join")
    public SimpleOk join(@PathVariable String roomId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        roomService.joinRoom(roomId, userId);
        return new SimpleOk(true);
    }

    //  Schedule Archive: 내가 속한 방 전부
    @GetMapping("/my")
    public MyRoomsResponse myRooms(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        var list = roomService.findMyRooms(userId);

        // =========================================================
        //  (선택지 1) 내가 속한 방들의 roomId를 모아 멤버를 한 번에 조회
        // =========================================================
        List<String> roomIds = list.stream() // [추가]
                .map(rm -> rm.getRoom().getRoomId()) // [추가]
                .distinct() // [추가]
                .collect(Collectors.toList()); // [추가]

        List<RoomMemberRepository.RoomMemberRow> memberRows =
                roomService.findMembersByRoomIds(roomIds);

        // roomId -> members(loginId, role) 로 그룹핑
        Map<String, List<MemberSummary>> membersByRoom =
                memberRows.stream().collect(Collectors.groupingBy(
                        RoomMemberRepository.RoomMemberRow::getRoomId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                r -> new MemberSummary(r.getLoginId(), r.getRole()),
                                Collectors.toList()
                        )
                ));

        var rooms = list.stream().map(rm -> {
            var room = rm.getRoom();
            String hostLoginId = (room.getHost() == null) ? null : room.getHost().getLoginId();

            // 해당 방 멤버들(없으면 빈 리스트)
            List<MemberSummary> members = membersByRoom.getOrDefault(room.getRoomId(), List.of()); // [추가]

            return new MyRoomItem(
                    room.getRoomId(),
                    hostLoginId,
                    rm.getRole(),
                    rm.getJoinedAt(),
                    room.getLastActiveAt(),
                    rm.isFavorite(), // [추가] 즐겨찾기 여부
                    members
            );
        }).collect(Collectors.toList());

        return new MyRoomsResponse(rooms);
    }

    // =========================================================
    // [추가] 즐겨찾기(별표) 토글 API
    // POST /api/rooms/{roomId}/favorite
    // =========================================================
    @PostMapping("/{roomId}/favorite") // [추가]
    public ToggleFavoriteResponse toggleFavorite(@PathVariable String roomId, HttpServletRequest request) { // [추가]
        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        boolean favorite = roomService.toggleFavorite(roomId, userId); // [추가]
        return new ToggleFavoriteResponse(roomId, favorite); // [추가]
    }

    //  단순 OK 응답
    public record SimpleOk(boolean ok) {}

    @DeleteMapping("/{roomId}")
    public SimpleOk deleteRoom(@PathVariable String roomId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);

        roomService.deleteRoom(roomId, userId);

        return new SimpleOk(true);
    }
}
