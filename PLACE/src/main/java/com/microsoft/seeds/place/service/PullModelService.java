package com.microsoft.seeds.place.service;

import com.microsoft.seeds.place.models.fsm.PullModelMenuAudioFiles;
import com.microsoft.seeds.place.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.place.models.request.CreatePullModelMenuAudioFileRequest;
import com.microsoft.seeds.place.repository.PullModelDataRepository;
import com.microsoft.seeds.place.repository.PullModelMenuAudioFilesRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.logging.Logger;

@Service
public class PullModelService {
    private static final Logger logger = Logger.getLogger(PullModelService.class.getName());
    @Autowired
    private PullModelMenuAudioFilesRepository pullModelMenuAudioFilesRepository;

    @Autowired
    private PullModelDataRepository pullModelDataRepository;

    public Optional<PullModelData> getDataById(String id){
        return pullModelDataRepository.findById(id);
    }

    public PullModelData saveData(PullModelData data){
        return pullModelDataRepository.save(data);
    }

    public Optional<PullModelMenuAudioFiles> getMenuAudioFilesById(int id){
        return pullModelMenuAudioFilesRepository.findById(id);
    }

    public void deleteMenuAudioFiles(int id){
        pullModelMenuAudioFilesRepository.deleteById(id);
    }

    public void saveMenuAudioFiles(PullModelMenuAudioFiles pullModelMenuAudioFiles){
        pullModelMenuAudioFilesRepository.save(pullModelMenuAudioFiles);
    }

    public void onReceiveMenuAudioFromMQ(JSONObject jsonObject){
        CreatePullModelMenuAudioFileRequest createPullModelMenuAudioFileRequest = CreatePullModelMenuAudioFileRequest.fromJSON(jsonObject);
        PullModelMenuAudioFiles pullModelMenuAudioFilesObj = new PullModelMenuAudioFiles();
        Optional<PullModelMenuAudioFiles> pullModelMenuAudioFilesFromDB = getMenuAudioFilesById(0);
        if(pullModelMenuAudioFilesFromDB.isPresent()){
            pullModelMenuAudioFilesObj = pullModelMenuAudioFilesFromDB.get();
            logger.info("DELETING PREVIOUS PULL MODEL AUDIO FILE WITH ID: " + pullModelMenuAudioFilesObj.getId());
            deleteMenuAudioFiles(pullModelMenuAudioFilesObj.getId());
        }
        logger.info("WRITING LATEST PULL MODEL AUDIO FILE WITH ID: " + pullModelMenuAudioFilesObj.getId());
        saveMenuAudioFiles(new PullModelMenuAudioFiles(
                pullModelMenuAudioFilesObj.getId(),
                createPullModelMenuAudioFileRequest
                        .getUpdatedPullModelMenuData(pullModelMenuAudioFilesObj.data, 0),
                createPullModelMenuAudioFileRequest.speechRates));
    }
}
