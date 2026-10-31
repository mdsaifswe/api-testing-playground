package com.playground.api.repository;

import com.playground.api.entity.SavedRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedRequestRepository extends JpaRepository<SavedRequestEntity, Long> {
    List<SavedRequestEntity> findByCollectionIdOrderByCreatedAtDesc(Long collectionId);
    List<SavedRequestEntity> findByCollectionIsNullOrderByCreatedAtDesc();
    List<SavedRequestEntity> findAllByOrderByCreatedAtDesc();
}
