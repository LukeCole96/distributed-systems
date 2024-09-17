package com.app.repository;

import com.app.entity.DbDowntimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbDowntimeStoreRepository extends JpaRepository<DbDowntimeEntity, Integer> {
}