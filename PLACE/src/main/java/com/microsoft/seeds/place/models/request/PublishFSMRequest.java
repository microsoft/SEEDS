package com.microsoft.seeds.place.models.request;

import com.microsoft.seeds.place.models.utils.Constants;

public class PublishFSMRequest {
    public String id;
    public String type;
    public String fsmType;

    public String getFsmType() {
        return fsmType;
    }

    public void setFsmType(String fsmType) {
        this.fsmType = fsmType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PublishFSMRequest() {
        fsmType = Constants.FSM_TYPE_STATIC;
    }
}
