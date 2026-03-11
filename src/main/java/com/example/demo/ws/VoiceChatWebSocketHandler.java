package com.example.demo.ws;

import com.example.demo.chat.api.dto.PlaceExtractResponse;
import com.example.demo.chat.application.ChatMessageService;
import com.example.demo.chat.application.PlaceExtractService;
import com.example.demo.chat.application.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

// [음성채팅 기능]
@Component
@RequiredArgsConstructor
public class VoiceChatWebSocketHandler extends TextWebSocketHandler {

    // [수정] null 필드 JSON에서 제거
    private final ObjectMapper om = new ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<String>> buffers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final PlaceExtractService placeExtractService;

    private final RoomService roomService;
    private final ChatMessageService chatMessageService;

    @Value("${app.place.extract.debounce-ms:2500}")
    private long debounceMs;

    @Value("${app.place.extract.max-lines:6}")
    private int maxLines;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = getQueryParam(session.getUri(), "roomId");
        if (roomId == null || roomId.isBlank()) roomId = "default";

        roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);

        //  입장 시 최근 채팅 히스토리 전송(카톡처럼)
        try {
            var recent = chatMessageService.findRecentAsc(roomId, 200);
            for (var m : recent) {
                sendToSession(session, OutMsg.chat(
                        m.getSender(),
                        m.getMessage(),
                        m.getCreatedAt().toString()
                ));
            }
        } catch (Exception ignored) {}

        broadcast(roomId, OutMsg.chat("SYSTEM", "입장했습니다.", OffsetDateTime.now().toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        InMsg in = om.readValue(message.getPayload(), InMsg.class);
        String roomId = (in.roomId == null || in.roomId.isBlank()) ? "default" : in.roomId;

        String sender = (in.sender == null || in.sender.isBlank()) ? "USER" : in.sender;
        String text = (in.text == null) ? "" : in.text;

        chatMessageService.save(roomId, sender, text); // 채팅 DB 저장
        roomService.touch(roomId);

        broadcast(roomId, OutMsg.chat(sender, text, OffsetDateTime.now().toString()));

        var q = buffers.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>());
        q.add(sender + ": " + text);

        if (q.size() >= maxLines) {
            cancelTimer(roomId);
            scheduler.execute(() -> runExtractAndBroadcast(roomId));
            return;
        }

        cancelTimer(roomId);
        timers.put(roomId, scheduler.schedule(() -> runExtractAndBroadcast(roomId), debounceMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = getQueryParam(session.getUri(), "roomId");
        if (roomId == null || roomId.isBlank()) roomId = "default";

        Set<WebSocketSession> set = rooms.get(roomId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                rooms.remove(roomId);
                buffers.remove(roomId);
                cancelTimer(roomId);
                timers.remove(roomId);
            }
        }
    }

    private void cancelTimer(String roomId) {
        ScheduledFuture<?> f = timers.get(roomId);
        if (f != null) f.cancel(false);
    }

    private void runExtractAndBroadcast(String roomId) {
        try {
            var q = buffers.get(roomId);
            if (q == null || q.isEmpty()) return;

            List<String> batch = new ArrayList<>();
            String line;
            while ((line = q.poll()) != null) {
                batch.add(line);
            }
            if (batch.isEmpty()) return;

            PlaceExtractResponse extracted = placeExtractService.extractPlaces(batch);
            // [추가] 추출 결과가 비어있으면 broadcast 하지 않음
            if (extracted == null || extracted.getPlaces() == null || extracted.getPlaces().isEmpty()) {
                return;
            }

            // 분류 없이 places만 브로드캐스트
            broadcast(roomId, OutMsg.places(extracted, OffsetDateTime.now().toString()));
        } catch (Exception ignored) {
        }
    }

    private void broadcast(String roomId, OutMsg out) {
        Set<WebSocketSession> set = rooms.getOrDefault(roomId, Collections.emptySet());
        String payload;
        try {
            payload = om.writeValueAsString(out);
        } catch (Exception e) {
            payload = "{\"type\":\"CHAT\",\"sender\":\"SYSTEM\",\"text\":\"serialize error\",\"ts\":\"\"}";
        }

        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(payload));
            } catch (Exception ignored) {}
        }
    }

    // 특정 세션에만 보내기(히스토리 복구용)
    private void sendToSession(WebSocketSession session, OutMsg out) {
        try {
            String payload = om.writeValueAsString(out);
            if (session.isOpen()) session.sendMessage(new TextMessage(payload));
        } catch (Exception ignored) {}
    }

    private String getQueryParam(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String pair : uri.getQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return decode(kv[1]);
        }
        return null;
    }

    private String decode(String v) {
        try { return java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return v; }
    }

    @Data
    static class InMsg {
        public String roomId;
        public String sender;
        public String text;
    }

    @Data
    static class OutMsg {
        public String type;

        // [CHAT]
        public String sender;
        public String text;

        //  분류 없는 누적 장소
        public List<String> places;

        public String ts;

        public static OutMsg chat(String sender, String text, String ts) {
            OutMsg m = new OutMsg();
            m.type = "CHAT";
            m.sender = sender;
            m.text = text;
            m.ts = ts;
            return m;
        }

        public static OutMsg places(PlaceExtractResponse r, String ts) {
            OutMsg m = new OutMsg();
            m.type = "PLACES";
            m.places = (r.getPlaces() == null) ? List.of() : r.getPlaces();
            m.ts = ts;
            return m;
        }
    }
}