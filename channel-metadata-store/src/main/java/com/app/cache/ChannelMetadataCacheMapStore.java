//package com.app.cache;
//
//import com.app.entity.ChannelMetadataEntity;
//import com.app.repository.ChannelMetadataRepository;
//import com.hazelcast.map.MapStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.Collection;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Component
//public class ChannelMetadataCacheMapStore implements MapStore<Long, ChannelMetadataEntity> {
//
//    @Autowired
//    private ChannelMetadataRepository repository;
//
//    @Override
//    public ChannelMetadataEntity load(Long key) {
//        return repository.findById(key).orElse(null);
//    }
//
//    @Override
//    public Map<Long, ChannelMetadataEntity> loadAll(Collection<Long> keys) {
//        return repository.findAllById(keys).stream()
//                .collect(Collectors.toMap(ChannelMetadataEntity::getId, e -> e));
//    }
//
//    @Override
//    public Iterable<Long> loadAllKeys() {
//        return repository.findAll().stream()
//                .map(ChannelMetadataEntity::getId)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void store(Long key, ChannelMetadataEntity value) {
//        repository.save(value);
//    }
//
//    @Override
//    public void storeAll(Map<Long, ChannelMetadataEntity> map) {
//        repository.saveAll(map.values());
//    }
//
//    @Override
//    public void delete(Long key) {
//        repository.deleteById(key);
//    }
//
//    @Override
//    public void deleteAll(Collection<Long> keys) {
//        repository.deleteAllById(keys);
//    }
//}
