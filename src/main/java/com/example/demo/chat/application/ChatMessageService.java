package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessageEntity;
import com.example.demo.chat.domain.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final RoomService roomService;
    private final ChatMessageRepository chatRepo;

    public void save(String roomId, String sender, String text) {
        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        ChatMessageEntity m = new ChatMessageEntity();
        m.setRoom(room);
        m.setSender(sender);
        m.setMessage(text);
        chatRepo.save(m);
    }

    public List<ChatMessageEntity> findRecentAsc(String roomId, int limit) {
        var desc = chatRepo.findByRoom_RoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit));
        return desc.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
    }

    public String buildChatLog(String roomId) {
        var all = chatRepo.findByRoom_RoomIdOrderByCreatedAtAsc(roomId);
        return all.stream()
                .map(m -> m.getSender() + ": " + m.getMessage())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }



}
