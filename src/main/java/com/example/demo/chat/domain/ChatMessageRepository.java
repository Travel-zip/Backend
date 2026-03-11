package com.example.demo.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByRoom_RoomIdOrderByCreatedAtAsc(String roomId);


    List<ChatMessageEntity> findByRoom_RoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);
}
