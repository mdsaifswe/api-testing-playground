package com.playground.api.repository;

import com.playground.api.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, Long> {
    List<EnvironmentEntity> findAllByOrderByCreatedAtDesc();

    Optional<EnvironmentEntity> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE EnvironmentEntity e SET e.isActive = false")
    void deactivateAll();
}
