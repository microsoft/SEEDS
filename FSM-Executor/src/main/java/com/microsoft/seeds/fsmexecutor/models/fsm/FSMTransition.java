package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.cache.LRUCache;
import com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton.StaticExpFSMCache;
import com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton.StaticOnDemandFSMDataCache;
import com.microsoft.seeds.fsmexecutor.models.request.FSMExecGetFSMRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.CustomIVRServerErrorException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.logging.Logger;

public class FSMTransition {
    private static final Logger logger = Logger.getLogger(FSMTransition.class.getName());
    private State source;
    private String targetFSMId;
    private String targetFSMType;
    private FSMAction action;

    private static ExchangeFilterFunction errorResponseFilter = ExchangeFilterFunction
            .ofResponseProcessor(FSMTransition::exchangeFilterResponseProcessor);

    public FSMTransition(State source, String targetFSMId, String targetFSMType, FSMAction action) {
        this.targetFSMId = targetFSMId;
        this.targetFSMType = targetFSMType;
        this.action = action;
        this.source = source;
    }

    public void execute(FSMContext fsmc, Object data){
        if(action != null)
            action.execute(fsmc, data);

        // CHECK THE LOCAL CACHE FOR FSM
        LRUCache<String, ExpFSM> fsmCache = StaticExpFSMCache.getInstance().getCache();
        LRUCache<String, OnDemandFSMData> onDemandFSMDataLRUCache = StaticOnDemandFSMDataCache.getInstance().getCache();
        Optional<ExpFSM> targetFSM = Optional.empty();

        if(targetFSMType.equalsIgnoreCase(Constants.FSM_TYPE_ON_DEMAND)){
            Optional<OnDemandFSMData> onDemandFSMDataOptional = onDemandFSMDataLRUCache.get(targetFSMId);
            if(onDemandFSMDataOptional.isPresent()){
                targetFSM = Optional.of(onDemandFSMDataOptional.get().getFSM());
            }
        }else{
            targetFSM = fsmCache.get(targetFSMId);
        }

        // IF NOT IN LOCAL CACHE FETCH FROM PLACE
        if(!targetFSM.isPresent()){
            logger.info("TRANSITION FSM NOT PRESENT IN CACHE");
            targetFSM = fetchTargetFSM(fsmc.getPlaceGetFSMByIdApi(), fsmc.getWebClientBuilder());
        }

        if(targetFSM.isPresent()){
            fsmc.getFsmContextController().updateExpFSMStackAndCurrStateMap(fsmc, targetFSM.get(),
                    fsmc.getCurrState());
            fsmc.setCurrState(targetFSM.get().getFirstState());
            fsmc.executeCurrentStateEntryAction(null);
        }
    }

    private Optional<ExpFSM> fetchTargetFSM(String url, WebClient.Builder webClientBuilder){
        LRUCache<String, ExpFSM> fsmCache = StaticExpFSMCache.getInstance().getCache();
        LRUCache<String, OnDemandFSMData> onDemandFSMDataLRUCache = StaticOnDemandFSMDataCache.getInstance().getCache();

        FSMExecGetFSMRequest fsmExecGetFSMRequest = new FSMExecGetFSMRequest(targetFSMId, targetFSMType);
        try {
            String response = webClientBuilder
                    .filter(errorResponseFilter)
                    .build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Mono.just(fsmExecGetFSMRequest), FSMExecGetFSMRequest.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
//            res.subscribe();
            JSONObject jsonObject = new JSONObject(response);
            ExpFSM res;
            if(targetFSMType.equalsIgnoreCase(Constants.FSM_TYPE_ON_DEMAND)){
                OnDemandFSMData onDemandFSMData = OnDemandFSMData.deserialize(jsonObject);
                onDemandFSMDataLRUCache.put(onDemandFSMData.getId(), onDemandFSMData); // STORE IN CACHE
                res = onDemandFSMData.getFSM();
            }else{
                res = FSMGeneratorAPI.deserializeFSM(jsonObject);
                fsmCache.put(res.getId(), res); // STORE IN CACHE
            }
            return Optional.of(res);
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        return Optional.empty();
    }

    private static Mono<ClientResponse> exchangeFilterResponseProcessor(ClientResponse response) {
        HttpStatus status = response.statusCode();
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new CustomIVRServerErrorException(body)));
        }
        if (HttpStatus.BAD_REQUEST.equals(status)) {
            return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new CustomIVRServerErrorException(body)));
        }
        return Mono.just(response);
    }

    public JSONObject toJSON(String eventName){
        JSONObject res = new JSONObject();
        res.put("event", eventName);
        res.put("source", source.getId());
        res.put("targetFSMId", targetFSMId);
        res.put("targetFSMType", targetFSMType);
        res.put("action", action.getInstanceArgs());
        return res;
    }
}
