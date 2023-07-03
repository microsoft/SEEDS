package com.microsoft.seeds.place.repository;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FSMRespository extends MongoRepository<ExpFSM, String> {
    public Optional<ExpFSM> findByIdAndType(String id, String type);
    public List<ExpFSM> findAll();
    public List<ExpFSM> findAllByType(String type);
}
