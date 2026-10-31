package com.playground.api.repository;

import com.playground.api.entity.RequestHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistoryEntity, Long> {
    Page<RequestHistoryEntity> findAllByOrderByExecutedAtDesc(Pageable pageable);
    List<RequestHistoryEntity> findTop50ByOrderByExecutedAtDesc();
}
