package com.playground.api.repository;

import com.playground.api.entity.EnvironmentVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariableEntity, Long> {
    List<EnvironmentVariableEntity> findByEnvironmentId(Long environmentId);
    void deleteByEnvironmentId(Long environmentId);
}
