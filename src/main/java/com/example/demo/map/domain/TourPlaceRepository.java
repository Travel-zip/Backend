package com.example.demo.map.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourPlaceRepository extends JpaRepository<TourPlaceEntity, Long> {

    List<TourPlaceEntity> findByRoom_RoomIdOrderByCreatedAtDesc(String roomId);

    boolean existsByRoom_RoomIdAndTitleAndLatAndLng(String roomId, String title, Double lat, Double lng);


}