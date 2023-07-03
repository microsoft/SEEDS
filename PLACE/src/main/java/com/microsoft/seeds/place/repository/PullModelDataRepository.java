package com.microsoft.seeds.place.repository;

import com.microsoft.seeds.place.models.fsm.pullmodel.PullModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PullModelDataRepository extends MongoRepository<PullModelData, String> {
    Optional<PullModelData> findById(String id);
}
