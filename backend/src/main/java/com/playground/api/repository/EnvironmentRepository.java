package com.playground.api.repository;

import com.playground.api.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, Long> {
    List<EnvironmentEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    Optional<EnvironmentEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    boolean existsByIdAndWorkspaceId(Long id, Long workspaceId);

    Optional<EnvironmentEntity> findByWorkspaceIdAndIsActiveTrue(Long workspaceId);

    Optional<EnvironmentEntity> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE EnvironmentEntity e SET e.isActive = false WHERE e.workspace.id = :workspaceId")
    void deactivateAllByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
