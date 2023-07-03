package com.microsoft.seeds.place.models.request;

import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;

public class CreatePullModelRequest {
    public String id;
    public String clientEp;
    public HashMap<String, List<String>> availableFSM;

    public WebClient.Builder webClientBuilder;
    public String userInputEp;

    public CreatePullModelRequest() {
    }

    public HashMap<String, List<String>> getAvailableFSM() {
        return availableFSM;
    }

    public void setAvailableFSM(HashMap<String, List<String>> availableFSM) {
        this.availableFSM = availableFSM;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientEp() {
        return clientEp;
    }

    public void setClientEp(String clientEp) {
        this.clientEp = clientEp;
    }

    public CreatePullModelRequest setUserInputEp(String userInputEp) {
        this.userInputEp = userInputEp;
        return this;
    }

    public CreatePullModelRequest setWebClientBuilder(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        return this;
    }


    @Override
    public String toString() {
        return "id: " + id + ", clientEp: " + clientEp + ", availableFSM: " + availableFSM.toString();
    }
}

