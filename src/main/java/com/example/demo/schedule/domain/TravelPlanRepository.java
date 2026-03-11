package com.example.demo.schedule.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {


    Optional<TravelPlanEntity> findTopByRoom_RoomIdOrderByCreatedAtDesc(String roomId);
}
