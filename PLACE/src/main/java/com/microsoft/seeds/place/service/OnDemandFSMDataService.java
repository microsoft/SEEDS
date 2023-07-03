package com.microsoft.seeds.place.service;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.RawData;
import com.microsoft.seeds.place.models.request.CreateQuizRequest;
import com.microsoft.seeds.place.models.request.CreateScrambleRequest;
import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.QuizSummary;
import com.microsoft.seeds.place.repository.OnDemandFSMDataRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OnDemandFSMDataService {
    private static final Logger logger = Logger.getLogger(OnDemandFSMDataService.class.getName());
    @Autowired
    private OnDemandFSMDataRepository onDemandFSMDataRepository;

    private final Clock clock = Clock.systemDefaultZone();

    public boolean isPresent(String id){
        return onDemandFSMDataRepository.existsById(id);
    }

    public Optional<OnDemandFSMData> findById(String id){
        return onDemandFSMDataRepository.findById(id);
    }

    public Optional<OnDemandFSMData> findByIdAndType(String id, String type){
        return onDemandFSMDataRepository.findByIdAndType(id, type);
    }

    public OnDemandFSMData save(OnDemandFSMData onDemandFSMData){
        logger.info("SAVED IN ON DEMAND : " + onDemandFSMData.serialize());
        return onDemandFSMDataRepository.save(onDemandFSMData);
    }

    public void delete(String id){
        logger.info("DELETING EXISTING ON DEMAND RECORD FOR ID: " + id);
        onDemandFSMDataRepository.deleteById(id);
    }

    public List<OnDemandFSMData> getAll(){
        return onDemandFSMDataRepository.findAll();
    }

    public List<OnDemandFSMData> getAllByType(String type){
        return onDemandFSMDataRepository.findAllByType(type);
    }

    public List<OnDemandFSMData> getAllByTypeAndLanguageSortedByTimestamp(String type, String language){
        return onDemandFSMDataRepository.findAllByType(type).stream()
                 .sorted((a, b) -> {
                     if (a.timeStamp < b.timeStamp) return 1;
                     if (a.timeStamp > b.timeStamp) return -1;
                     return 0;
                 })
                 .filter(onDemandFSMData ->
                         onDemandFSMData.data.has("language") && onDemandFSMData.data.getString("language")
                                 .equalsIgnoreCase(language))
                .collect(Collectors.toList());
    }

    public void onReceiveQuizExpFromMQ(CreateQuizRequest createQuizRequest){
        OnDemandFSMData onDemandFSMData = new OnDemandFSMData(
                createQuizRequest.getId(),
                Constants.QUIZ_FSM_TYPE,
                createQuizRequest.toJSON(),
                1,
                clock.millis()
        );
        // DELETE PREVIOUS IF EXISTS
        findById(onDemandFSMData.id)
                .ifPresent(data -> {
                    onDemandFSMData.setVersion(data.getVersion() + 1);
                    onDemandFSMData.setTimeStamp(data.timeStamp);
                    delete(data.id);
                });
        onDemandFSMDataRepository.save(onDemandFSMData);
    }

    public List<RawData> getRawDataListFromQuizList(String jsonStr){
        JSONObject json = new JSONObject(jsonStr);
        JSONArray dataArray = json.getJSONArray("data");
        List<OnDemandFSMData> onDemandFSMDataList = getAll();
        List<RawData> res = new ArrayList<>();
        for(int i = 0; i<dataArray.length(); ++i){
            JSONObject quizJson = dataArray.getJSONObject(i);
            onDemandFSMDataList.forEach(onDemandFSMData -> {
                CreateQuizRequest inDBReq = CreateQuizRequest.fromJSON(onDemandFSMData.data);
                if(quizJson.getString("title").equals(inDBReq.title)){
                    RawData rawData = inDBReq.getRawData();
                    rawData.data.put("questions", quizJson.getJSONArray("questions"));
                    List<List<String>> fullOptions = new ArrayList<>();
                    for(int questionNum = 0; questionNum < inDBReq.questions.size(); ++questionNum) {
                        List<String> options = quizJson.getJSONArray("options")
                                .getJSONArray(questionNum)
                                .toList()
                                .stream()
                                .map(item -> String.valueOf(item))
                                .collect(Collectors.toList());
                        int correctAnswer = inDBReq.correctAnswers.get(questionNum);
                        if(correctAnswer != 0){
                            Collections.swap(options, 0, correctAnswer);
                        }
                        fullOptions.add(options);
                    }
                    rawData.data.put("options", fullOptions);
                    rawData.data.put("correctAnswers", IntStream.generate(() -> 0).limit(fullOptions.size()).toArray());
                    res.add(rawData);
                }
            });
        }
        return res;
    }

    public JSONObject createScramble(CreateScrambleRequest createScrambleRequest){
        OnDemandFSMData onDemandFSMData = new OnDemandFSMData(
                createScrambleRequest.getId(),
                Constants.SCRAMBLE_FSM_TYPE,
                createScrambleRequest.toJSON(),
                1,
                clock.millis());
        return save(onDemandFSMData).serialize();
    }

    public void saveDummyQuizzes(){
        for (CreateQuizRequest cq : CreateQuizRequest.getListOfDummyRequests()) {
            OnDemandFSMData onDemandFSMData = new OnDemandFSMData(
                    cq.getTitle(),
                    Constants.QUIZ_FSM_TYPE,
                    cq.toJSON(),
                    1, clock.millis());
            save(onDemandFSMData);
        }
    }

    public List<QuizSummary> getAllQuizSummaryByLanguage(String language){
        return getAllByTypeAndLanguageSortedByTimestamp(Constants.QUIZ_FSM_TYPE, language)
                .stream()
                .map(onDemandFSMData -> {
                    CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(onDemandFSMData.data);
                    QuizSummary res = new QuizSummary(createQuizRequest.id, createQuizRequest.title, createQuizRequest.language , true);
                    if(createQuizRequest.getTitleAudio() != null){
                        res.setTitleAudio(AudioFileWithSpeechRates.getFor(createQuizRequest.titleAudio));
                    }
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<String> getAllScrambleIDsByLanguage(String language){
        return getAllByTypeAndLanguageSortedByTimestamp(Constants.SCRAMBLE_FSM_TYPE, language)
                .stream()
                .map(onDemandFSMData -> onDemandFSMData.id)
                .collect(Collectors.toList());
    }
}
