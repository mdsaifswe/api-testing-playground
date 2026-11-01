package com.playground.api.repository;

import com.playground.api.entity.RequestHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistoryEntity, Long> {
    Page<RequestHistoryEntity> findByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId, Pageable pageable);
    List<RequestHistoryEntity> findTop50ByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId);
    Optional<RequestHistoryEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Modifying
    @Query("DELETE FROM RequestHistoryEntity h WHERE h.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
