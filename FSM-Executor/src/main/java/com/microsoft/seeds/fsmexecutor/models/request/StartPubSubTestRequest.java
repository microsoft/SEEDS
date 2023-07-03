package com.microsoft.seeds.fsmexecutor.models.request;

public class StartPubSubTestRequest {
    public String fsmContextId;
    public int pubNum;
    public String experienceName;

    public StartPubSubTestRequest() {
    }

    public String getFsmContextId() {
        return fsmContextId;
    }

    public void setFsmContextId(String fsmContextId) {
        this.fsmContextId = fsmContextId;
    }

    public int getPubNum() {
        return pubNum;
    }

    public void setPubNum(int pubNum) {
        this.pubNum = pubNum;
    }

    public String getExperienceName() {
        return experienceName;
    }

    public void setExperienceName(String experienceName) {
        this.experienceName = experienceName;
    }
}
