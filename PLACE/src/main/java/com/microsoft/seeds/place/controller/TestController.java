package com.microsoft.seeds.place.controller;

import com.microsoft.seeds.place.models.cache.LRUCache;
import com.microsoft.seeds.place.models.cache.cachesingleton.StaticExpFSMCache;
import com.microsoft.seeds.place.models.cache.cachesingleton.StaticOnDemandFSMDataCache;
import com.microsoft.seeds.place.models.fsm.*;
import com.microsoft.seeds.place.models.request.FSMExecGetFSMRequest;
import com.microsoft.seeds.place.models.request.StartFSMRequest;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.CustomResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("test")
public class TestController {
    @Autowired
    private WebClient.Builder webClientBuilder;
    private LRUCache<String, ExpFSM> fsmCache;
    private LRUCache<String, OnDemandFSMData> onDemandFSMDataCache;
    private String userInputEp = "http://localhost:8080/test/userInput";

    private String placeBaseURL = "http://localhost:8080/";

    private String placeLatestVersionApi = "getLatestVersion";

    private String placeGetFSMApi = "fsmExecGetFSM";

    @PostConstruct
    public void initializeController(){
        fsmCache  = StaticExpFSMCache.getInstance().getCache();
        onDemandFSMDataCache = StaticOnDemandFSMDataCache.getInstance().getCache();
    }


    private static final Logger logger = Logger.getLogger(TestController.class.getName());
    @PostMapping(value = "/userInput", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> userInput(@RequestBody ClientInput input){
        logger.info(input.toJSON().toString());
        if(input.fsmContextId == null || input.event == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("INVALID INPUT"), HttpStatus.BAD_REQUEST);
        }
        FSMContext fsmContext = FSMContextInExecution.getInstance().get(input.fsmContextId);
        if(fsmContext == null){
            return new ResponseEntity<>(CustomResponse.getErrorResponse("FSM Context not in execution"), HttpStatus.BAD_REQUEST);
        }
        fsmContext.dispatch(input.event, null);
        return new ResponseEntity<>(CustomResponse.getSuccessResponse(), HttpStatus.OK);
    }

    @PostMapping(value = "/startFSM", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap> startFSM(@RequestBody StartFSMRequest startFSMReq){
        logger.info("START FSM REQ: " + startFSMReq.toJSON());
        if(FSMContextInExecution.getInstance().isPresent(startFSMReq.getFsmContextId())){
            logger.info("ERROR: ALREADY RUNNING");
            return new ResponseEntity<>(CustomResponse.getErrorResponse("ALREADY RUNNING"), HttpStatus.BAD_REQUEST);
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
//                .setClientEp(startFSMReq.getClientEp())
                .setWebClientBuilder(webClientBuilder)
                .setUserInputEp(userInputEp);
        FSMContextInExecution.getInstance().add(fsmContext);
        fsmContext.initialize();
        return new ResponseEntity<>(CustomResponse.getSuccessResponse("type", "started"), HttpStatus.ACCEPTED);
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
}
