package com.example.demo.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//  UserRepository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}
