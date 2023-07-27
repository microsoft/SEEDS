package com.microsoft.seeds.fsmexecutor.controller;

import com.microsoft.seeds.fsmexecutor.models.StartFSMReqQueue;
import com.microsoft.seeds.fsmexecutor.models.cache.LRUCache;
import com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton.StaticExpFSMCache;
import com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton.StaticOnDemandFSMDataCache;
import com.microsoft.seeds.fsmexecutor.models.fsm.generators.QuizGenerator;
import com.microsoft.seeds.fsmexecutor.models.fsm.generators.ScrambleGenerator;
import com.microsoft.seeds.fsmexecutor.models.request.CreateQuizRequest;
import com.microsoft.seeds.fsmexecutor.models.request.CreateScrambleRequest;
import com.microsoft.seeds.fsmexecutor.models.request.FSMExecGetFSMRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.CustomResponse;
import com.microsoft.seeds.fsmexecutor.models.fsm.*;
import com.microsoft.seeds.fsmexecutor.models.request.StartFSMRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.PullModelTestConstants;
import com.microsoft.seeds.fsmexecutor.models.utils.WriteToFile;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class FSMExecutorController {

    @Autowired
    private WebClient.Builder webClientBuilder;
    private static Logger logger = Logger.getLogger(FSMExecutorController.class.getName());
//    private static final String SUBSCRIPTION_NAME_FSM = "fsm-executor-local";
    private static final String FSM_TOPIC = "fsm";

    private static final String CLIENT_STATES_ARRAY_KEY = "states";

    private String userInputEp;
    private final Clock clock = Clock.systemDefaultZone();

    private LRUCache<String, ExpFSM> fsmCache;

    private LRUCache<String, OnDemandFSMData> onDemandFSMDataCache;
    private StartFSMReqQueue startFSMReqQueue;

    private int testInputIndex = 0;
    private StringBuilder testClientStateStrBuilder;

    @Value("${seeds.place.deployed.url}")
    private String placeBaseURL;

    @Value("${seeds.place.get.latest.version.api}")
    private String placeLatestVersionApi;

    @Value("${seeds.place.get.fsm.api}")
    private String placeGetFSMApi;

    @Value("${seeds.place.get.fsm.byid.api}")
    private String getPlaceGetFSMApiById;

    @Value("${FSM_EXEC_PORT}")
    private String port;

    @PostConstruct
    public void initializeController(){
        userInputEp = "http://localhost:" + port + "/userInput";
        logger.info("USER INPUT URL: " + userInputEp);
        fsmCache  = StaticExpFSMCache.getInstance().getCache();
        onDemandFSMDataCache = StaticOnDemandFSMDataCache.getInstance().getCache();
//        startFSMReqQueue = new StartFSMReqQueue(Constants.START_FSM_REQ_QUEUE_TIMEOUT);

//        for(CreateQuizRequest quizRequest : CreateQuizRequest.getListOfDummyRequests()){
//            OnDemandFSMData onDemandFSMData = new OnDemandFSMData(quizRequest.id, Constants.QUIZ_FSM_TYPE, quizRequest.toJSON(), 1);
//            onDemandFSMDataCache.put(onDemandFSMData.id, onDemandFSMData);
//        }

        CreateScrambleRequest createScrambleRequest = CreateScrambleRequest.getDummyRequest();
        fsmCache.put(createScrambleRequest.id, ScrambleGenerator.getFSM(createScrambleRequest));

        CreateQuizRequest createQuizRequest = CreateQuizRequest.getFromDummyJSON();
        OnDemandFSMData onDemandFSMData = new OnDemandFSMData(createQuizRequest.id, Constants.QUIZ_FSM_TYPE,
                createQuizRequest.toJSON(), 1, clock.millis());
        onDemandFSMDataCache.put(onDemandFSMData.id, onDemandFSMData);
    }

    @GetMapping(value = "/testOnDemandQuizFSM", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<HashMap<String, Object>> testOnDemandQuiz(@RequestParam String id){
        Optional<OnDemandFSMData> onDemandFSMDataOptional = onDemandFSMDataCache.get(id);
        if(onDemandFSMDataOptional.isPresent()){
            return new ResponseEntity<>(CustomResponse.getObjResponse(
                    FSMGeneratorAPI.serialiseFSM(QuizGenerator.getFSMFromOnDemandFSMData(onDemandFSMDataOptional.get()))),
                    HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(CustomResponse.getErrorResponse("INVALID ID"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/inCacheIds", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<String, Object>> getInCacheIds(@RequestParam String cacheType){
        Set<String> allowedCacheTypes = new HashSet<>(Arrays.asList(Constants.FSM_TYPE_ON_DEMAND, Constants.FSM_TYPE_STATIC));
        if(allowedCacheTypes.contains(cacheType)){
            JSONObject res = new JSONObject();
            if (Constants.FSM_TYPE_ON_DEMAND.equals(cacheType)) {
                res.put("onDemandFSMData", onDemandFSMDataCache.getAllKeys());
                return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
            }
            res.put("fsm", fsmCache.getAllKeys());
            return new ResponseEntity<>(CustomResponse.getObjResponse(res), HttpStatus.OK);
        }
        return new ResponseEntity<>(
                CustomResponse
                        .getErrorResponse("cacheType should be in : " +
                                String.join(", ", allowedCacheTypes)), HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/onDemandFSMData", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<String, Object>> getOnDemandFSMDataFromCache(@RequestParam String id){
        Optional<OnDemandFSMData> onDemandFSMDataOptional = onDemandFSMDataCache.get(id);
        return onDemandFSMDataOptional
                .map(onDemandFSMData -> new ResponseEntity<>(CustomResponse.getObjResponse(onDemandFSMData.serialize()), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("NOT PRESENT IN ON DEMAND FSM CACHE"),
                        HttpStatus.BAD_REQUEST));
    }

    @GetMapping(value = "/fsm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<String, Object>> getFsmFromCache(@RequestParam String id){
        Optional<ExpFSM> fsmOptional = fsmCache.get(id);
        return fsmOptional
                .map(expFSM -> new ResponseEntity<>(CustomResponse.getObjResponse(FSMGeneratorAPI.serialiseFSM(expFSM)), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(CustomResponse.getErrorResponse("NOT PRESENT IN FSM CACHE"), HttpStatus.BAD_REQUEST));
    }

//    @JmsListener(destination = FSM_TOPIC, containerFactory = "topicJmsListenerContainerFactory",
//            subscription = "${messagequeue.subsname}")
//    public void receiveFSM(String message) {
//        //TODO : Parse FSM into an object and
//        // save in memory indexed with FSM ID,
//        // if already present => discard, check version
//        logger.info(message);
//        String id;
//        int version;
//        try {
//            JSONObject jsonObject = new JSONObject(message);
//            switch (jsonObject.getString(Constants.FSM_TYPE_KEY)){
//                case Constants.FSM_TYPE_ON_DEMAND:
//                    OnDemandFSMData onDemandFSMData = OnDemandFSMData.deserialize(jsonObject);
//                    onDemandFSMDataCache.put(onDemandFSMData.id, onDemandFSMData);
//                    id = onDemandFSMData.id;
//                    version = onDemandFSMData.version;
//                    logger.info("STORED IN ON DEMAND FSM CACHE");
//                    break;
//                default:
//                    ExpFSM fsm = FSMGeneratorAPI.deserializeFSM(jsonObject);
//                    fsmCache.put(fsm.getId(), fsm);
//                    id = fsm.getId();
//                    version = fsm.getVersion();
//                    logger.info("STORED IN FSM CACHE");
//                    break;
//            }
//            Optional<List<StartFSMRequest>> queuedReqs = startFSMReqQueue.get(id, version);
//            if(queuedReqs.isPresent()){
//                logger.info("QUEUED REQs ARE PRESENT FOR " + id + " VERSION: " + version);
//                for(StartFSMRequest startFSMRequest : queuedReqs.get()){
//                    logger.info("CALLING START FSM FOR CONTEXT ID: " + startFSMRequest.fsmContextId + " FSM NAME: " + startFSMRequest.fsmName);
//                    startFSM(startFSMRequest);
//                }
//            }
//        }catch (JSONException err){
//            logger.info(err.toString());
//        }
//    }

    @PostMapping(value = "/userInput", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> userInput(@RequestBody ClientInput input){
        //TODO: Parse client input, see if its valid,
        // see if FSM is in execution,
        // dispatch new event and
        // call the registered ep of the client server to spit new state (sendNewState()) => HAPPENS IN PUSHFSMSTATEACTION
        logger.info(input.toJSON().toString());
        if(input.fsmContextId == null || input.event == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("INVALID INPUT"), HttpStatus.BAD_REQUEST);
        }
        FSMContext fsmContext = FSMContextInExecution.getInstance().get(input.fsmContextId);
        if(fsmContext == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("FSM Context not in execution"), HttpStatus.BAD_REQUEST);
        }

        fsmContext.dispatch(input.event, null);
        return new ResponseEntity<>(CustomResponse.getObjResponse(getCurrStatesResponse(fsmContext)), HttpStatus.OK);
    }

    @PostMapping(value = "/startFSM", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> startFSM(@RequestBody StartFSMRequest startFSMReq){
        logger.info("START FSM REQ: " + startFSMReq.toJSON());
        if(FSMContextInExecution.getInstance().isPresent(startFSMReq.getFsmContextId())){
            logger.info("ERROR: ALREADY RUNNING");
            return new ResponseEntity<>(CustomResponse.getErrorResponse("ALREADY RUNNING"), HttpStatus.BAD_REQUEST);
        }
        if(!FSMContextInExecution.getInstance().canAdd()){
            logger.info("FSMContextInExecution Memory limit reached!");
            return new ResponseEntity<>(CustomResponse.getErrorResponse("MAX NUMBER OF RUNNING FSMs REACHED!"), HttpStatus.FORBIDDEN);
        }
//        if(startFSMReqQueue.isContextIdPresent(startFSMReq.fsmContextId)){
//            logger.info("ERROR: CONTEXT ID ALREADY QUEUED");
//            return new ResponseEntity<>(CustomResponse.getErrorResponse("CONTEXT ID ALREADY QUEUED"), HttpStatus.BAD_REQUEST);
//        }
        FSMExecGetFSMRequest fsmExecGetFSMRequest = new FSMExecGetFSMRequest(startFSMReq.fsmName, startFSMReq.type, startFSMReq.fsmType);
        Optional<ExpFSM> fsmOptional = fsmCache.get(startFSMReq.getFsmName());
        Optional<OnDemandFSMData> onDemandFSMDataOptional = onDemandFSMDataCache.get(startFSMReq.getFsmName());

        // NOT PRESENT IN EITHER CACHE
        if(!fsmOptional.isPresent() && !onDemandFSMDataOptional.isPresent()){
            // GET DATA FROM PLACE
            boolean success = getAndCacheFSMFromPLACE(fsmExecGetFSMRequest);
            if(!success){
                return new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH FSM"), HttpStatus.BAD_REQUEST);
            }
        }else{
            int inCacheLatestVersion = fsmOptional.isPresent() ? fsmOptional.get().getVersion() : onDemandFSMDataOptional.get().getVersion();
            int latestFSMVersionFromPLACE = getLatestFSMVersionFromPLACE(fsmExecGetFSMRequest);
            if(latestFSMVersionFromPLACE == -1){
                return new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH FSM"), HttpStatus.BAD_REQUEST);
            }

            // FETCH DATA FROM PLACE
            if(latestFSMVersionFromPLACE > inCacheLatestVersion){
                boolean success = getAndCacheFSMFromPLACE(fsmExecGetFSMRequest);
                if(!success){
                    return new ResponseEntity<>(CustomResponse.getErrorResponse("NO SUCH FSM"), HttpStatus.BAD_REQUEST);
                }
            }
        }

        Optional<ExpFSM> fsmOptionalUpdated = fsmCache.get(startFSMReq.getFsmName());
        Optional<OnDemandFSMData> onDemandFSMDataOptionalUpdated = onDemandFSMDataCache.get(startFSMReq.getFsmName());

        FSMContext fsmContext = (fsmOptionalUpdated.isPresent() ?
                FSMContextFactory.getFSMContext(fsmOptionalUpdated.get(), startFSMReq.fsmContextId) :
                FSMContextFactory.getFSMContext(onDemandFSMDataOptionalUpdated.get(), startFSMReq.fsmContextId))
                .setPlaceGetFSMByIdApi(placeBaseURL + getPlaceGetFSMApiById)
                .setWebClientBuilder(webClientBuilder)
                .setUserInputEp(userInputEp);
        FSMContextInExecution.getInstance().add(fsmContext);
        fsmContext.initialize();
        return new ResponseEntity<>(CustomResponse.getObjResponse(getCurrStatesResponse(fsmContext)), HttpStatus.ACCEPTED);
    }

    @PostMapping(value="/serializeDeserializeScramble")
    public ResponseEntity<HashMap> serializeDeserialize(){
        CreateScrambleRequest createScrambleRequest = CreateScrambleRequest.getDummyRequest();
        ExpFSM fsm = ScrambleGenerator.getFSM(createScrambleRequest);
        JSONObject serialized = FSMGeneratorAPI.serialiseFSM(fsm);
        ExpFSM deserialized = FSMGeneratorAPI.deserializeFSM(serialized);
        return new ResponseEntity<>(CustomResponse.getObjResponse(serialized), HttpStatus.OK);
    }

    //TODO: 1. CREATE APIs to make more stories and Quiz FSMs ==> in PLACE
    // 2. /startPullModel API should take a list of user specific FSMs which should be included in the experience
    // 3. Automatically generate ComposableFSMContextExecutor based on the above list

//    @PostMapping(value = "/startPullModel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<HashMap> startPullModel(@RequestBody CreatePullModelRequest createPullModelRequest){
//        if(ComposableFSMInExecution.getInstance().isPresent(createPullModelRequest.getId())){
//            return new ResponseEntity<>(CustomResponse.getErrorResponse("ALREADY RUNNING"), HttpStatus.BAD_REQUEST);
//        }
////        ComposableFSMContextExecutor dummyComposable = ComposableFSMExecutorGenerator.getDummyComposableFSMContextExecutor(
////                restTemplate, startComposableFSMRequest.getClientEp(), userInputEp);
////        ComposableFSMInExecution.getInstance().add(dummyComposable);
//        // TODO: CHECK IF ALL THE FSMs ARE PRESENT
//        createPullModelRequest
//                .setWebClientBuilder(webClientBuilder)
//                .setUserInputEp(userInputEp);
//        logger.info(createPullModelRequest.toString());
//        ComposableFSMContextExecutor composableFSMContextExecutor = ComposableFSMExecutorGenerator
//                .getComposableFSMContextExecutor(createPullModelRequest);
//        ComposableFSMInExecution.getInstance().add(composableFSMContextExecutor);
//        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.ACCEPTED);
//    }

    @PostMapping(value = "/stopFSM", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> stopFSM(@RequestParam String fsmContextId){
        FSMContextInExecution.getInstance().remove(fsmContextId);
        AutoStateDispatchThread.getInstance().stopExecution(fsmContextId);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @PostMapping(value = "/stopAllFSM", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> stopAllFSM(){
        FSMContextInExecution.getInstance().removeAll();
        AutoStateDispatchThread.getInstance().stopAllExecution();
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }
    @GetMapping(value = "/startPullModelTest")
    public void startPullModelTest() {
        testInputIndex = 0;
        testClientStateStrBuilder = new StringBuilder();
        StartFSMRequest startFSMRequest = new StartFSMRequest();
        startFSMRequest.setFsmName("PullModel_Kannada");
        startFSMRequest.setFsmContextId("st12345");
        startFSMRequest.setFsmType("static");
        startFSMRequest.setType("Pull Model");

        testClientStateStrBuilder.append("\n------------------- STARTFSM REQ ------------------------\n");
        testClientStateStrBuilder.append(startFSMRequest.toJSON().toString(4));

        JSONObject initialStatesJSON = new JSONObject(startFSM(startFSMRequest).getBody());
        testClientStateStrBuilder.append("\n-------------------- NEW STATES --------------------\n");
        testClientStateStrBuilder.append(initialStatesJSON.toString(4));

        List<String> inputList = PullModelTestConstants.INPUTS_QUIZ;

        while(testInputIndex < inputList.size()){
            if (!inputList.get(testInputIndex)
                    .equalsIgnoreCase(PullModelTestConstants.NO_INPUT)) {
                ClientInput input = new ClientInput();
                input.setFsmContextId(startFSMRequest.fsmContextId);
                input.setEvent(inputList.get(testInputIndex));
                testClientStateStrBuilder.append("\n------------------- INPUT ------------------------\n");
                testClientStateStrBuilder.append(input.toJSON().toString(4));
                JSONObject statesJSON = new JSONObject(userInput(input).getBody());
                if (statesJSON.getJSONArray(CLIENT_STATES_ARRAY_KEY).length() != 0) {
                    testClientStateStrBuilder.append("\n-------------------- NEW STATES --------------------\n");
                    testClientStateStrBuilder.append(statesJSON.toString(4));
                } else {
                    testClientStateStrBuilder.append("\n----------------------------------------------------\n");
                    testClientStateStrBuilder.append("Will not receive any further states. \n" + "EXIT. ");
                    WriteToFile.write(testClientStateStrBuilder.toString());
                    break;
                }
            }
            testInputIndex++;
        }
    }

    private JSONObject getCurrStatesResponse(FSMContext fsmContext){
        JSONObject res = new JSONObject();
        res.put(CLIENT_STATES_ARRAY_KEY, fsmContext.getClientStateListToPush()
                .stream()
                .map(ClientState::toJSON)
                .collect(Collectors.toList()));
        fsmContext.clearClientStateListToPush();
        return res;
    }

    int getLatestFSMVersionFromPLACE(FSMExecGetFSMRequest request){
        try {
            String response = webClientBuilder
                    .build()
                    .post()
                    .uri(placeBaseURL + placeLatestVersionApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), FSMExecGetFSMRequest.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            assert response != null;
            JSONObject data = new JSONObject(response);
            return data.getInt("version");
        }catch (Exception ex){
            logger.info(ex.getMessage());
        }
        return -1;
    }

    boolean getAndCacheFSMFromPLACE(FSMExecGetFSMRequest request){
        try {
            String dataStr = webClientBuilder
                    .build()
                    .post()
                    .uri(placeBaseURL + placeGetFSMApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), FSMExecGetFSMRequest.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            assert dataStr != null;
            JSONObject data = new JSONObject(dataStr);
            if(request.getFsmType().equals(Constants.FSM_TYPE_ON_DEMAND)){
                logger.info(dataStr);
                OnDemandFSMData onDemandFSMData = OnDemandFSMData.deserialize(data);
                onDemandFSMDataCache.put(onDemandFSMData.id, onDemandFSMData);
                logger.info("FSM " + request.id + " STORED IN ON DEMAND CACHE");
            }else{
                ExpFSM fsm = FSMGeneratorAPI.deserializeFSM(data);
                fsmCache.put(fsm.getId(), fsm);
                logger.info("FSM " + request.id + " STORED IN FSM CACHE");
            }
            return true;
        }catch (Exception ex){
            logger.info(ex.getMessage());
        }
        return false;
    }


}
