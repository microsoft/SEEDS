package com.microsoft.seeds.place.repository;

import com.microsoft.seeds.place.models.fsm.RawData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RawDataRepository extends MongoRepository<RawData, String> {
    public Optional<RawData> findById(String id);
    public List<RawData> findAllByType(String type);
}
