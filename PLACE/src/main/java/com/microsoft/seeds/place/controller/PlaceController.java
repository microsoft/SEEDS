package com.microsoft.seeds.place.controller;

import com.microsoft.seeds.place.models.fsm.generators.*;
import com.microsoft.seeds.place.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.place.models.utils.PullModelDummyDataGen;
import com.microsoft.seeds.place.models.utils.QuizSummary;
import com.microsoft.seeds.place.models.fsm.*;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.request.*;
import com.microsoft.seeds.place.models.utils.CustomResponse;
import com.microsoft.seeds.place.service.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class PlaceController {

    @Autowired
    private WebClient.Builder webClientBuilder;
    private static final Logger logger = Logger.getLogger(PlaceController.class.getName());
//    private static final String SUBSCRIPTION_NAME_AUDIO_FILES = "seeds-message-bus-acs-queue-temp";
    private static final String AUDIO_FILES_TOPIC = "queue-t";
    private static final String FSM_TOPIC = "fsm";
    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private FSMService fsmService;

    @Autowired
    private PullModelService pullModelService;

    @Autowired
    private HTTPService httpService;

    @Autowired
    private RawDataService rawDataService;

    @Autowired
    private OnDemandFSMDataService onDemandFSMDataService;

    private final Clock clock = Clock.systemDefaultZone();

    @JmsListener(destination = "${servicebus.experience.queue.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveExperiences(byte[] message){
        try{
            String s = new String(message, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(s);
            logger.info("RECEIVED EXPERIENCE IN QUEUE : " + jsonObject);
            if(jsonObject.has("type")){
                String type = jsonObject.getString("type").toLowerCase();
                if(type.equalsIgnoreCase(Constants.QUIZ_FSM_TYPE)){
                    CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(jsonObject);
                    ExpFSM fsm = QuizGenerator.getFSM(createQuizRequest, 1);
                    if(fsm != null){
                        rawDataService.onReceiveExperienceFromMQ(createQuizRequest.getId()); // SET ISPROCESSED TO TRUE
                        onDemandFSMDataService.onReceiveQuizExpFromMQ(createQuizRequest); // UPDATE THE ONDEMANDDATA DB
                    }
                    else{
                        logger.info("QUIZ REQUEST NOT VALID!");
                    }
                }
                else if(type.equalsIgnoreCase(DeleteQuizAzureFunctionRequest.DELETE_QUIZ_AZURE_FUNCTION_REQ_TYPE)) {
                    String id = jsonObject.getString("id");
                    rawDataService.delete(id);
                    onDemandFSMDataService.delete(id);
                }
                else if(type.equalsIgnoreCase(Constants.PULL_MODEL_MENU_ITEM_MESSAGE_TYPE)){
                    pullModelService.onReceiveMenuAudioFromMQ(jsonObject);
                }
            }
        }catch (Exception ex){
            logger.info(ex.getMessage());
        }
    }



    @DeleteMapping(value="/byFSMId")
    @ResponseBody
    ResponseEntity<HashMap<String, Object>> deleteFSMById(@RequestParam String id){
        fsmService.delete(id);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @DeleteMapping(value = "/byOnDemandId", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> deleteOnDemandById(@RequestParam String id){
        onDemandFSMDataService.delete(id);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @DeleteMapping(value="/allFSM")
    ResponseEntity<HashMap<String, Object>> deleteAllFSM(){
        fsmService.deleteAll();
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @DeleteMapping(value = "/byId")
    ResponseEntity<HashMap<String, Object>> deleteById(@RequestParam String id, @RequestParam String type){
        switch (type.toLowerCase()){
            case Constants.QUIZ_FSM_TYPE:
                if(!onDemandFSMDataService.isPresent(id) || !rawDataService.isPresent(id)){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("QUIZ DOES NOT EXIST"), HttpStatus.BAD_REQUEST);
                }
                rawDataService.setIsProcessed(id, false);
                CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(
                        onDemandFSMDataService.findById(id).get().getData());
                DeleteQuizAzureFunctionRequest deleteQuizAzureFunctionRequest = new DeleteQuizAzureFunctionRequest(createQuizRequest.id);
                logger.info("CALLING ACS FUNCTION WITH PAYLOAD: " + deleteQuizAzureFunctionRequest.toJSON());
                httpService.deleteQuizAudios(deleteQuizAzureFunctionRequest); // CALLING HTTP AZURE FUNCTION TO DELETE THE QUIZ AUDIOS
                return new ResponseEntity<>(CustomResponse.getSuccessResponse(),
                        HttpStatus.ACCEPTED);
            default:
                return new ResponseEntity<>(CustomResponse.getErrorResponse("LOGIC FOR TYPE: " + type + " IS NOT DEFINED"), HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping(value = "allFSMByType")
    ResponseEntity<HashMap<String, Object>> getAllFSMByType(@RequestParam String type){
        return new ResponseEntity<>(CustomResponse
                .getObjResponse(getFSMListJSON(fsmService.getAllByType(type))),
                HttpStatus.OK);
    }

    @GetMapping(value = "/allFSM")
    ResponseEntity<HashMap<String, Object>> getAllFSM(){
        return new ResponseEntity<>(CustomResponse.getObjResponse(getFSMListJSON(fsmService.getAll())),
                HttpStatus.OK);
    }

    @GetMapping(value = "/rawDataById")
    ResponseEntity<HashMap<String, Object>> getRawDataById(@RequestParam String id){
        Optional<RawData> rawData = rawDataService.getByIdForWebpage(id);
        return rawData.map(data ->
                new ResponseEntity<>(CustomResponse.getObjResponse(data.processBasedOnType().getData()), HttpStatus.OK))
                .orElseGet(() ->
                        new ResponseEntity<>(CustomResponse.getErrorResponse("Raw data with id " + id + " does not exist"), HttpStatus.BAD_REQUEST));
    }

    @GetMapping(value = "/getAllRawData")
    ResponseEntity<List<HashMap>> getAllRawData(){
        List<RawData> rawData = rawDataService.getAllSortedByTimestamp();
        List<HashMap> res = new ArrayList<>();
        for (RawData rawDatum : rawData) {
            res.add((HashMap) rawDatum.toJSON().toMap());
        }
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/copyAudioData")
    ResponseEntity<HashMap<String, Object>> copyAudioData(){
        fsmService.deleteAll();
        long time = clock.millis();
        List<RawData> toSaveRawData = new ArrayList<>();
        List<ExpFSM> toSaveExpFsm = new ArrayList<>();
        for (CreateAudioFSMRequest createAudioFSMRequest : httpService.GetRequestForAudioContent()) {
            createAudioFSMRequest.resetAudioFiles();
            RawData rawData = new RawData(createAudioFSMRequest.getId(), createAudioFSMRequest.getType(), createAudioFSMRequest.toJSON(), true, time);
            toSaveRawData.add(rawData);
            ExpFSM fsm = AudioFSMGenerator.getFSM(createAudioFSMRequest);
            toSaveExpFsm.add(fsm);
        }
        rawDataService.saveAll(toSaveRawData);
        fsmService.saveAll(toSaveExpFsm);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @GetMapping(value= "/testPullModelData")
    ResponseEntity<HashMap<String, Object>> testPullModelData(){
        PullModelData pullModelData = PullModelDataGenerator.getPullModelData(
                Constants.KANNADA_LANG, fsmService.getAll(), onDemandFSMDataService.getAll(), rawDataService.getAll());
        pullModelService.saveData(pullModelData);
        System.out.println(pullModelData.getVisualizationString());
        System.out.println(pullModelData.getNumOptionsFromNode(pullModelData.getRootNode()));
        ExpFSM fsm = PullModelFSMGenerator.getFSM(pullModelData, Constants.KANNADA_LANG);
        return new ResponseEntity<>(CustomResponse.getObjResponse(FSMGeneratorAPI.serialiseFSM(fsmService.save(fsm))), HttpStatus.OK);
    }

    @PostMapping(value = "/savePullModelData")
    ResponseEntity<HashMap<String, Object>> savePullModelData(){
        return new ResponseEntity<>(CustomResponse.getObjResponse(
                pullModelService.saveData(PullModelDummyDataGen.getDummy()).toJSON()), HttpStatus.OK);
    }

    @GetMapping(value="/pullModelData")
    ResponseEntity<HashMap<String, Object>> getPullModelData(@RequestParam @NotNull String id){
        return pullModelService.getDataById(id).map(data -> new ResponseEntity<>(CustomResponse.getObjResponse(data.toJSON()), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("DOES NOT EXIST"), HttpStatus.BAD_REQUEST));
    }

    @GetMapping(value="/pullModelDataGraph")
    ResponseEntity<HashMap<String, Object>> pullModelDataVisualization(@RequestParam @NotNull String id){
        Optional<PullModelData> pullModelDataOptional = pullModelService.getDataById(id);
        if(!pullModelDataOptional.isPresent()){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH PULLMODEL DATA"), HttpStatus.BAD_REQUEST);
        }
        JSONObject res = new JSONObject();
        res.put("id", pullModelDataOptional.get().getId());
        res.put("graph", pullModelDataOptional.get().getVisualizationString());
        logger.info("PULL MODEL DATA GRAPH WITH ID: " + id + "\n" + res.getString("graph"));
        return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
    }

    @DeleteMapping(value="/rawDataById")
    ResponseEntity<HashMap<String, Object>> deleteRawDataById(@RequestParam String id){
        rawDataService.delete(id);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @PostMapping(value = "/populateRawData", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> populateRawData(@RequestBody String jsonStr){
        rawDataService.saveAll(onDemandFSMDataService.getRawDataListFromQuizList(jsonStr));
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @PostMapping(value = "/setIsProcessedToTrue")
    ResponseEntity<HashMap<String, Object>> setIsProcessedToTrue(){
        rawDataService
                .getAll()
                .forEach(rawData -> rawDataService.setIsProcessed(rawData.id, true));
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }


    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> create(@RequestBody String requestStr){
        JSONObject requestJSON = new JSONObject(requestStr);
        if(requestJSON.has("type")){
            switch (requestJSON.getString("type")){
                case Constants.QUIZ_FSM_TYPE:
                    return createQuiz(requestJSON);
                default:
                    return new ResponseEntity<>(CustomResponse
                            .getErrorResponse("Creation logic for type " + requestJSON.getString("type") + " is not defined."),
                            HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }else{
            return new ResponseEntity<>(CustomResponse.getErrorResponse("type key not defined"), HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<HashMap<String, Object>> createQuiz(JSONObject quizJSON){
        CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(quizJSON);
        ExpFSM fsm = QuizGenerator.getFSM(createQuizRequest, 1);
        if(fsm == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("Invalid Input"), HttpStatus.BAD_REQUEST);
        }
        RawData rawData = createQuizRequest.getRawData();
        rawData.setTimeStamp(clock.millis());
        rawDataService.onCreateRequest(rawData);

        // CALLING HTTP AZURE FUNCTION TO CREATE THE QUIZ AUDIOS
        logger.info("CALLING AZURE FUNCTION TO CREATE AUDIO FILES WITH PAYLOAD: " + quizJSON);
        httpService.createQuizAudios(quizJSON);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(),
                HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/createScramble", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> createScramble(@RequestBody CreateScrambleRequest createScrambleRequest){
        if(!createScrambleRequest.isValid()){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("Bad Request"),
                    HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(
                CustomResponse.getObjResponse(onDemandFSMDataService.createScramble(createScrambleRequest)),
                HttpStatus.CREATED);
    }

    @PostMapping(value = "/createDummyQuizzes", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> createDummyQuizzes(){
        onDemandFSMDataService.saveDummyQuizzes();
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(),
                HttpStatus.CREATED);
    }

    @PostMapping(value = "/createStory", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> createStory(@RequestBody CreateStoryRequest createStoryRequest){
        ExpFSM fsm = StoryGenerator.getFSM(createStoryRequest);
        if(fsm == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("INVALID INPUT"), HttpStatus.BAD_REQUEST);
        }
//        DummyDatabase.getInstance().addFSM(createStoryFromTextRequest.getId(), fsm);
        return new ResponseEntity<>(CustomResponse.getObjResponse(FSMGeneratorAPI.serialiseFSM(fsmService.save(fsm))),
                HttpStatus.CREATED);
    }
    @PostMapping(value = "/populateAudioFSMData")
    ResponseEntity<HashMap<String, Object>> populateAudioFSMData(){
        fsmService.populateAudioFSMData();
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.CREATED);
    }

    @PostMapping(value = "/createAudioFsm", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> createAudioFsm(@RequestBody CreateAudioFSMRequest createAudioFSMRequest){
        return fsmService.createAudioFsm(createAudioFSMRequest)
                .map(data -> new ResponseEntity<>(CustomResponse.getObjResponse(FSMGeneratorAPI.serialiseFSM(data)), HttpStatus.CREATED))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("ID or AUDIO FILES IS NULL!"), HttpStatus.BAD_REQUEST));
    }

    @GetMapping(value = "/onDemandFSMData")
    @ResponseBody
    ResponseEntity<HashMap<String, Object>> getOnDemandFSMData(@RequestParam String id, @RequestParam String type){
        return onDemandFSMDataService
                .findByIdAndType(id, type)
                .map(demandFSMData ->
                        new ResponseEntity<>(CustomResponse.getObjResponse(demandFSMData.serialize()), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH onDemandFSMData EXISTS"), HttpStatus.BAD_REQUEST));
    }


    @GetMapping(value="/fsm")
    ResponseEntity<HashMap<String, Object>> getExperience(@RequestParam String id, @RequestParam String type){
        return fsmService.getByIdAndType(id, type)
                .map(data -> new ResponseEntity<>(CustomResponse.getObjResponse(FSMGeneratorAPI.serialiseFSM(data)), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH FSM EXISTS"), HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping(value="/fsm")
    ResponseEntity<HashMap<String, Object>> deleteFSM(@RequestParam String id){
        fsmService.delete(id);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @PostMapping(value = "/fsm", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> saveExperience(@RequestBody Map<String, Object> body){
        return new ResponseEntity<>(CustomResponse.getObjResponse(
                FSMGeneratorAPI.serialiseFSM(fsmService.save(body))), HttpStatus.CREATED);
    }

    @PostMapping(value = "/publishFSM", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> publishFSM(@RequestBody PublishFSMRequest publishFSMRequest){
        String res;
        int version;
        switch (publishFSMRequest.getFsmType()){
            case Constants.FSM_TYPE_ON_DEMAND:
                Optional<OnDemandFSMData> onDemandFSMData = onDemandFSMDataService.findByIdAndType(publishFSMRequest.id, publishFSMRequest.type);
                if(onDemandFSMData.isPresent()){
                    version = onDemandFSMData.get().getVersion();
                    res = onDemandFSMData.get().serialize().toString();
                }else{
                   return new ResponseEntity<>(CustomResponse.getErrorResponse("fsm does not exist"), HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                Optional<ExpFSM> fsmOptional = fsmService.getByIdAndType(publishFSMRequest.id, publishFSMRequest.type);
                if(fsmOptional.isPresent()){
                    res = FSMGeneratorAPI.serialiseFSM(fsmOptional.get()).toString();
                    version = fsmOptional.get().getVersion();
                }else{
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("fsm does not exist"), HttpStatus.BAD_REQUEST);
                }
                break;
        }

        logger.info(res);
        jmsTemplate.convertAndSend(FSM_TOPIC, res);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse("version", version), HttpStatus.OK);
    }

    @PostMapping(value = "/fsmExecGetFSMById", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> fsmExecGetFSMById(@RequestBody FSMExecGetFSMRequest fsmExecGetFSMRequest){
        JSONObject res;
        switch (fsmExecGetFSMRequest.getFsmType()){
            case Constants.FSM_TYPE_ON_DEMAND:
                Optional<OnDemandFSMData> onDemandFSMData = onDemandFSMDataService.findById(fsmExecGetFSMRequest.id);
                if(!onDemandFSMData.isPresent()){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("No such onDemandFSM"), HttpStatus.BAD_REQUEST);
                }
                res = onDemandFSMData.get().serialize();
                break;
            default:
                Optional<ExpFSM> fsm = fsmService.findById(fsmExecGetFSMRequest.id);
                if(!fsm.isPresent()){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("No such fsm"), HttpStatus.BAD_REQUEST);
                }
                res = FSMGeneratorAPI.serialiseFSM(fsm.get());
                break;
        }
        return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
    }

    @PostMapping(value = "/fsmExecGetFSM", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> fsmExecGetFSM(@RequestBody FSMExecGetFSMRequest fsmExecGetFSMRequest){
        JSONObject res;
        switch (fsmExecGetFSMRequest.getFsmType()){
            case Constants.FSM_TYPE_ON_DEMAND:
                Optional<OnDemandFSMData> onDemandFSMData = onDemandFSMDataService.findByIdAndType(fsmExecGetFSMRequest.id, fsmExecGetFSMRequest.type);
                if(!onDemandFSMData.isPresent()){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("No such onDemandFSM"), HttpStatus.BAD_REQUEST);
                }
                res = onDemandFSMData.get().serialize();
                break;
            default:
                Optional<ExpFSM> fsm = fsmService.getByIdAndType(fsmExecGetFSMRequest.id, fsmExecGetFSMRequest.type);
                if(!fsm.isPresent()){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("No such fsm"), HttpStatus.BAD_REQUEST);
                }
                res = FSMGeneratorAPI.serialiseFSM(fsm.get());
                break;
        }
        return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
    }

    @PostMapping(value = "/getLatestVersion", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> getLatestVersion(@RequestBody FSMExecGetFSMRequest fsmExecGetFSMRequest){
        JSONObject res = new JSONObject();
        String versionKey = "version";
        res.put(versionKey, -1);
        switch (fsmExecGetFSMRequest.getFsmType()){
            case Constants.FSM_TYPE_ON_DEMAND:
                Optional<OnDemandFSMData> onDemandFSMData = onDemandFSMDataService.findByIdAndType(fsmExecGetFSMRequest.id, fsmExecGetFSMRequest.type);
                return onDemandFSMData.map(demandFSMData -> new ResponseEntity<>(CustomResponse.getSuccessResponse(versionKey, demandFSMData.getVersion()), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("No such onDemandFSM"), HttpStatus.BAD_REQUEST));
            default:
                Optional<ExpFSM> fsm = fsmService.getByIdAndType(fsmExecGetFSMRequest.id, fsmExecGetFSMRequest.type);
                return fsm.map(expFSM -> new ResponseEntity<>(CustomResponse.getSuccessResponse(versionKey, expFSM.getVersion()), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("No such fsm"), HttpStatus.BAD_REQUEST));
        }
    }

    @PostMapping(value = "/populateTimeStampInRawData")
    ResponseEntity<HashMap<String, Object>> populateTimeStampInRawData(){
        List<RawData> rawDataList = rawDataService.getAllByType(Constants.QUIZ_FSM_TYPE);
        HashMap<String, OnDemandFSMData> onDemandFSMDataMap = new HashMap<>(
                onDemandFSMDataService
                .getAllByType(Constants.QUIZ_FSM_TYPE)
                .stream()
                .collect(Collectors.toMap(OnDemandFSMData::getId, item -> item)));
        rawDataList.forEach(rawData -> {
            if(onDemandFSMDataMap.containsKey(rawData.id)){
                OnDemandFSMData onDemandFSMData = onDemandFSMDataMap.get(rawData.id);
                rawData.setTimeStamp(onDemandFSMData.timeStamp);
            }
        });
        rawDataService.saveAll(rawDataList);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @GetMapping(value="/getAllQuizzes")
    ResponseEntity<HashMap<String, Object>> getAllQuizzes(){
        List<QuizSummary> quizList = rawDataService
                .getAllByTypeAndSortedByTimestamp(Constants.QUIZ_FSM_TYPE)
                .stream()
                .map(rawData -> {
                    CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(rawData.data);
                    return new QuizSummary(createQuizRequest.id, createQuizRequest.title, createQuizRequest.language , rawData.isProcessed);
                }).collect(Collectors.toList());
        JSONObject res = new JSONObject();
        res.put("quizzes", QuizSummary.getJSONArray(quizList));
        return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
    }

    @GetMapping(value="/getAllQuizzesByLanguage")
    ResponseEntity<HashMap<String, Object>> getAllQuizzesByLanguage(@RequestParam @NotNull @NotEmpty String language){
        JSONObject res = new JSONObject();
        res.put("quizzes", QuizSummary.getJSONArray(onDemandFSMDataService.getAllQuizSummaryByLanguage(language)));
        return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
    }

    @GetMapping(value="/test", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<HashMap<String, Object>> testQuiz(@RequestBody Map<String, Object> body){
        JSONObject audioData = new JSONObject((HashMap) body).getJSONObject("data");
        List<OnDemandFSMData> allFSMs = onDemandFSMDataService.getAll();
        for (OnDemandFSMData onDemandFSMData : allFSMs) {
            boolean hasFoundKn = false;
            JSONArray knObject = audioData.getJSONArray("kn");
            for(Object item : knObject){
                JSONObject itemObj = (JSONObject) item;
                String knId = itemObj.getString("id");
                String titleURL = itemObj.getString("url");
                if(onDemandFSMData.getId().equalsIgnoreCase(knId)){
                    CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(onDemandFSMData.data);
                    createQuizRequest.setTitleAudio(titleURL);
                    onDemandFSMData.setData(createQuizRequest.toJSON());
                    hasFoundKn = true;
                    break;
                }
            }

            if(!hasFoundKn){
                JSONArray enObject = audioData.getJSONArray("en");
                for(Object item : enObject){
                    JSONObject itemObj = (JSONObject) item;
                    String enId = itemObj.getString("id");
                    String titleURL = itemObj.getString("url");
                    if(onDemandFSMData.getId().equalsIgnoreCase(enId)){
                        CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(onDemandFSMData.data);
                        createQuizRequest.setTitleAudio(titleURL);
                        onDemandFSMData.setData(createQuizRequest.toJSON());
                        hasFoundKn = true;
                        break;
                    }
                }
            }

            if(!hasFoundKn){
                logger.info("URL NOT FOUND FOR ID: " + onDemandFSMData.getId() + " TITLE: " + onDemandFSMData.data.getString("title"));
            }
            onDemandFSMDataService.save(onDemandFSMData);
        }
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @GetMapping(value="/getAllScrambleByLanguage")
    ResponseEntity<HashMap<String, Object>> getAllScrambleByLanguage(@RequestParam @NotNull @NotEmpty String language){
        return new ResponseEntity<>(CustomResponse.getObjResponse(
                getJSONFromList("scramble", onDemandFSMDataService.getAllScrambleIDsByLanguage(language))), HttpStatus.OK);
    }

    private JSONObject getFSMListJSON(List<ExpFSM> fsms){
        JSONObject res = new JSONObject();
        JSONArray array = new JSONArray();
        for(ExpFSM fsm: fsms){
            array.put(FSMGeneratorAPI.serialiseFSM(fsm));
        }
        res.put("FSMList", array);
        return res;
    }

    private JSONObject getJSONFromList(String key, List<String> value){
        JSONObject res = new JSONObject();
        res.put(key, value);
        return res;
    }
}
