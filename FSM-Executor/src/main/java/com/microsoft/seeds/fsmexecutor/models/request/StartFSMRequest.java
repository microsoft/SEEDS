package com.microsoft.seeds.fsmexecutor.models.request;

import org.json.JSONObject;

public class StartFSMRequest {
    public String fsmName;
    public String fsmContextId;
    public String type;
    public String fsmType;
    public StartFSMRequest() {
    }

    public String getFsmType() {
        return fsmType;
    }

    public void setFsmType(String fsmType) {
        this.fsmType = fsmType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFsmName() {
        return fsmName;
    }

    public void setFsmName(String fsmName) {
        this.fsmName = fsmName;
    }

    public String getFsmContextId() {
        return fsmContextId;
    }

    public void setFsmContextId(String fsmContextId) {
        this.fsmContextId = fsmContextId;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("fsmName", fsmName);
        res.put("fsmContextId", fsmContextId);
        res.put("type", type);
        res.put("fsmType", fsmType);
        return res;
    }
}
