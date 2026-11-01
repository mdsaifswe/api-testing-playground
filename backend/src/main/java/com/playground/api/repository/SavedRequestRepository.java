package com.playground.api.repository;

import com.playground.api.entity.SavedRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedRequestRepository extends JpaRepository<SavedRequestEntity, Long> {
    List<SavedRequestEntity> findByCollectionIdAndWorkspaceIdOrderByCreatedAtDesc(Long collectionId, Long workspaceId);
    List<SavedRequestEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<SavedRequestEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
