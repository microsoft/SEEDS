package com.microsoft.seeds.place.service;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.place.models.fsm.generators.AudioFSMGenerator;
import com.microsoft.seeds.place.models.request.CreateAudioFSMRequest;
import com.microsoft.seeds.place.models.utils.CustomResponse;
import com.microsoft.seeds.place.repository.FSMRespository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FSMService {
    @Autowired
    private FSMRespository fsmRespository;

    private static final Logger logger = Logger.getLogger(FSMService.class.getName());

    public Optional<ExpFSM> findById(String id){
        return fsmRespository.findById(id);
    }

    public void delete(String id){
        fsmRespository.deleteById(id);
        logger.info("DELETED FSM WITH ID: " + id);
    }

    public void deleteAll(){
        fsmRespository.deleteAll();
        logger.info("DELETED ALL FSM DATA");
    }

    public List<ExpFSM> getAllByType(String type){
        return fsmRespository.findAllByType(type);
    }

    public List<ExpFSM> getAll(){
        return fsmRespository.findAll();
    }

    public ExpFSM save(ExpFSM fsm){
        logger.info("SAVING FSM FOR ID: " + fsm.getId());
        return fsmRespository.save(fsm);
    }

    public void saveAll(List<ExpFSM> fsmList){
        fsmList.forEach(item -> logger.info("SAVING EXP FSM WITH ID: " + item.getId()));
        fsmRespository.saveAll(fsmList);
    }

    public void populateAudioFSMData(){
        List<CreateAudioFSMRequest> requestList = CreateAudioFSMRequest.getAutomatedRequest();
        for(CreateAudioFSMRequest audioFSMRequest :  requestList){
            ExpFSM fsm = AudioFSMGenerator.getFSM(audioFSMRequest);
            fsmRespository.save(fsm);
        }
    }

    public Optional<ExpFSM> createAudioFsm(CreateAudioFSMRequest createAudioFSMRequest){
        if(createAudioFSMRequest.isValid()) {
            ExpFSM fsm = AudioFSMGenerator.getFSM(createAudioFSMRequest);
            return Optional.of(fsmRespository.save(fsm));
        }else{
            return Optional.empty();
        }
    }

    public Optional<ExpFSM> getByIdAndType(String id, String type){
        return fsmRespository.findByIdAndType(id, type);
    }

    public ExpFSM save(Map<String, Object> data){
        JSONObject jsonObject = new JSONObject(data);
        ExpFSM fsm = FSMGeneratorAPI.deserializeFSM(jsonObject);
        return fsmRespository.save(fsm);
    }
}
