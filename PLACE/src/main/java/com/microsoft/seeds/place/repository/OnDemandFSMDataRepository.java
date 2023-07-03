package com.microsoft.seeds.place.repository;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OnDemandFSMDataRepository extends MongoRepository<OnDemandFSMData, String> {
    public Optional<OnDemandFSMData> findByIdAndType(String id, String type);
    public List<OnDemandFSMData> findAll();
    public List<OnDemandFSMData> findAllByType(String type);
}
