package com.microsoft.seeds.place.models.request;

public class StartComposableFSMRequest {
    public String id;
    public String userId;
    public String clientEp;

    public StartComposableFSMRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientEp() {
        return clientEp;
    }

    public void setClientEp(String clientEp) {
        this.clientEp = clientEp;
    }
}
