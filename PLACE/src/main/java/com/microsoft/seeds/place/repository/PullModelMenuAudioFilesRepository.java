package com.microsoft.seeds.place.repository;

import com.microsoft.seeds.place.models.fsm.PullModelMenuAudioFiles;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PullModelMenuAudioFilesRepository extends MongoRepository<PullModelMenuAudioFiles, String> {
    public Optional<PullModelMenuAudioFiles> findById(int id);
    public void deleteById(int id);
}
