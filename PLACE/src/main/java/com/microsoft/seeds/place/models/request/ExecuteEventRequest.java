package com.microsoft.seeds.place.models.request;

public class ExecuteEventRequest {
    private String fsmContextId;
    private String event;

    public ExecuteEventRequest() {
    }

    public String getFsmContextId() {
        return fsmContextId;
    }

    public void setFsmContextId(String fsmContextId) {
        this.fsmContextId = fsmContextId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}
