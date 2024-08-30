package com.app.repository;

import com.app.entity.ChannelMetadataEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelMetadataRepository extends JpaRepository<ChannelMetadataEntity, Long> {
//    List<ChannelMetadataEntity> getAllChannelMetadata(String territory);
//    ChannelMetadataEntity findChannelMetadata(long id);
// ^ not needed, repository gives service an ability to find info by ID
}