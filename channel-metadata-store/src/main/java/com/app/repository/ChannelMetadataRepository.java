package com.app.repository;

import com.app.entity.ChannelMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelMetadataRepository extends JpaRepository<ChannelMetadataEntity, Long> {
    ChannelMetadataEntity findByCountryCode(String countryCode);

    //    List<ChannelMetadataEntity> getAllChannelMetadata(String territory);
//    ChannelMetadataEntiaty findChannelMetadata(long id);
// ^ not needed, repository gives service an ability to find info by ID
}