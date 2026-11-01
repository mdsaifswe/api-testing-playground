package com.playground.api.repository;

import com.playground.api.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionEntity, Long> {
    List<CollectionEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<CollectionEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByIdAndWorkspaceId(Long id, Long workspaceId);
}
