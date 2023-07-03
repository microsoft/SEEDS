package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.ClientState;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.CustomIVRServerErrorException;
import com.microsoft.seeds.fsmexecutor.models.utils.PlaceHolderAudioFileWithSpeechRates;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class PushFSMStateAction implements FSMAction {
    private String stateTopic = "states";
    private Logger logger = Logger.getLogger(PushFSMStateAction.class.getName());
    private static ExchangeFilterFunction errorResponseFilter = ExchangeFilterFunction
            .ofResponseProcessor(PushFSMStateAction::exchangeFilterResponseProcessor);
    @Override
    public void execute(FSMContext fsmc, Object data) {
//        String clientEp = fsmc.getClientEp();
        logger.info("QUEUING NEW STATE");
        ClientState state = fsmc.getCurrState().getClientState(); // GET INITIAL CLIENT STATE OBJECT FROM CURR STATE DATA
        state.setEventsFromList( // SET CLIENT STATE AUDIO EVENTS
                fsmc
                .getFsmContextController()
                .getClientStateEvents(fsmc, fsmc.getCurrState().getEventNames()));
        // IF STATE DATA CONTAINS STRING THAT NEEDS REPLACEMENT
        if(state.data != null && state.data.containsKey(Constants.AUDIO_DATA_KEY)) {
//            String newText = StringInterpolator.getInterpolatedString((String) state.data.get(Constants.STATE_DATA_TEXT_KEY), fsmc);
//            state.data.put(Constants.STATE_DATA_TEXT_KEY, newText);
            JSONObject stateDataJSON = new JSONObject(state.data);
            JSONArray audioDataJSONArray = stateDataJSON.getJSONArray(Constants.AUDIO_DATA_KEY);
            JSONArray res = new JSONArray();
            audioDataJSONArray.forEach(obj -> {
                JSONObject jsonObject = (JSONObject) obj;
                if(PlaceHolderAudioFileWithSpeechRates.isPlaceHolder(jsonObject)){
                    res.put(PlaceHolderAudioFileWithSpeechRates.getRealFor(jsonObject, fsmc).toJSON());
                }else{
                    res.put(jsonObject);
                }
            });
            stateDataJSON.put(Constants.AUDIO_DATA_KEY, res);
            state.data = stateDataJSON.toMap();
        }
        state.setFsmContextId(fsmc.getId());
        state.setType(fsmc.getType());
        state.setUserInputEp(fsmc.getUserInputEp());
        state.setOpsEventsFromList(fsmc.getOpsEvents());
        logger.info(state.toJSON().toString());
        fsmc.addToClientStateList(state);
//        try {
//            WebClient.Builder webClientBuilder = fsmc.getWebClientBuilder();
//            String response = webClientBuilder
//                    .filter(errorResponseFilter)
//                    .build()
//                    .post()
//                    .uri(clientEp)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .accept(MediaType.TEXT_HTML)
//                    .body(Mono.just(state), ClientState.class)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
////            res.subscribe();
//        }catch (Exception ex){
//            logger.severe(ex.getMessage());
//        }

        //RestTemplate restTemplate = fsmc.getRestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(state.toJSON().toMap(), headers);
//        try {
//            restTemplate.postForEntity(clientEp, entity, String.class);
//        }catch (RestClientException ex){
//            logger.severe(ex.getMessage());
//        }
//       JmsTemplate jmsTemplate = FsmexecutorApplication.applicationContext.getBean(JmsTemplate.class);
//        ClientState state = fsmc.getCurrState().getClientState();
//        state.setFsmContextId(fsmc.getId());
//        jmsTemplate.convertAndSend(stateTopic, state.toJSON().toString());
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("PushFSMStateAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new PushFSMStateAction();
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        json.put(JSON_NAME_KEY, getActionName().get(0));
        array.put(json);
        return array;
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
}
