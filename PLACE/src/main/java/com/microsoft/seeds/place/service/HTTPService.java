package com.microsoft.seeds.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.seeds.place.models.request.CreateAudioFSMRequest;
import com.microsoft.seeds.place.models.request.DeleteQuizAzureFunctionRequest;
import com.microsoft.seeds.place.models.utils.http.HTTPRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class HTTPService {
    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final Logger logger = Logger.getLogger(HTTPService.class.getName());

    @Value("${acs.base.url}")
    private String acsBaseURL;

    private void asyncPOSTRequest(HTTPRequest request){
        try{
            webClientBuilder
                    .build()
                    .post()
                    .uri(request.getUrl())
                    .contentType(request.getContentType())
                    .accept(request.getAccept())
                    .body(Mono.just(request.getBody()), request.getBodyClass())
                    .retrieve()
                    .bodyToMono(request.getResponseClass())
                    .subscribe(res ->
                            request.getAysncHTTPSubscriber().subscriber(res, request.getUrl()));
        }catch (Exception ex){
            logger.info(ex.getMessage());
        }
    }

    private void asyncPOSTRequest(HTTPRequest request, JSONObject body){
        try{
            webClientBuilder
                    .build()
                    .post()
                    .uri(request.getUrl())
                    .contentType(request.getContentType())
                    .accept(request.getAccept())
                    .body(BodyInserters.fromValue(body.toMap()))
                    .retrieve()
                    .bodyToMono(request.getResponseClass())
                    .subscribe(res ->
                            request.getAysncHTTPSubscriber().subscriber(res, request.getUrl()));
        }catch (Exception ex){
            logger.info(ex.getMessage());
        }
    }

    public List<CreateAudioFSMRequest> GetRequestForAudioContent(){
        String response =  webClientBuilder
                .build()
                .get()
                .uri("https://seeds-teacherapp.azurewebsites.net//content")
                .accept(MediaType.APPLICATION_JSON)
                .header("authToken", "postman")
                .retrieve()
                .bodyToMono(String.class).block();
        JSONObject jsonObj = new JSONObject("{ \"data\" : "  +response + "}");
        return jsonObj.getJSONArray("data")
                .toList()
                .stream()
                .map(json -> CreateAudioFSMRequest.fromJSON(new JSONObject((HashMap) json)))
                .collect(Collectors.toList());
    }

    public void deleteQuizAudios(DeleteQuizAzureFunctionRequest deleteQuizAzureFunctionRequest){
        HTTPRequest httpRequest = new HTTPRequest(acsBaseURL, DeleteQuizAzureFunctionRequest.class, deleteQuizAzureFunctionRequest);
        asyncPOSTRequest(httpRequest);
    }

    public void createQuizAudios(JSONObject quizJSON){
        HTTPRequest httpRequest = new HTTPRequest(acsBaseURL, null, null);
        asyncPOSTRequest(httpRequest, quizJSON);
    }

}
