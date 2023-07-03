package com.microsoft.seeds.place.service;

import com.microsoft.seeds.place.controller.PlaceController;
import com.microsoft.seeds.place.models.fsm.RawData;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.CustomResponse;
import com.microsoft.seeds.place.repository.RawDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RawDataService {
    private static final Logger logger = Logger.getLogger(RawDataService.class.getName());
    @Autowired
    private RawDataRepository rawDataRepository;

    public boolean isPresent(String id){
        return rawDataRepository.existsById(id);
    }

    public void save(RawData rawData){
        logger.info("SAVING RAW DATA FOR ID: " + rawData.id);
        rawDataRepository.save(rawData);
    }

    public void saveAll(List<RawData> list){
        list.forEach(item -> logger.info("SAVING RAW DATA WITH ID: " + item.id));
        rawDataRepository.saveAll(list);
    }

    public void delete(String id){
        logger.info("DELETING RAW DATA FOR ID: " + id);
        rawDataRepository.deleteById(id);
    }

    public void setIsProcessed(String id, boolean val){
        findById(id).ifPresent(data -> {
            data.setProcessed(val);
            save(data);
        });
    }

    public Optional<RawData> findById(String id){
        return rawDataRepository.findById(id);
    }

    public Optional<RawData> getByIdForWebpage(String id) {
        Optional<RawData> rawData = findById(id);
        rawData.ifPresent(data -> data.getData().put("type", data.type));
        rawData.ifPresent(data -> data.getData().put("isProcessed", data.isProcessed));
        return rawData;
    }

    public List<RawData> getAll(){
        return rawDataRepository.findAll();
    }

    public List<RawData> getAllSortedByTimestamp(){
        return getAll()
                .stream().sorted((a, b) -> {
                    if (a.timeStamp < b.timeStamp) return 1;
                    if (a.timeStamp > b.timeStamp) return -1;
                    return 0;
                })
                .collect(Collectors.toList());
    }

    public List<RawData> getAllByType(String type){
        return rawDataRepository.findAllByType(type);
    }

    public List<RawData> getAllByTypeAndSortedByTimestamp(String type){
        return getAllByType(type)
                .stream()
                .sorted((a, b) -> {
                    if (a.timeStamp < b.timeStamp) return 1;
                    if (a.timeStamp > b.timeStamp) return -1;
                    return 0;
                })
                .collect(Collectors.toList());
    }

    public void onReceiveExperienceFromMQ(String id){
        setIsProcessed(id, true);
    }

    public void onCreateRequest(RawData rawData){
        findById(rawData.id).ifPresent(data -> {
            logger.info("UPDATE RAW DATA REQ FOR ID " + rawData.id + " Original TimeStamp: " + data.timeStamp);
            rawData.setTimeStamp(data.timeStamp);
        });
        save(rawData);
    }

}
