package com.microsoft.seeds.fsmexecutor.models.fsm;

import org.json.JSONObject;

public class ClientInput {
    public String fsmContextId;
    public String event;

    public ClientInput() {
    }

    public ClientInput(String fsmContentId, String event) {
        this.fsmContextId = fsmContentId;
        this.event = event;
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

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("fsmContextId", fsmContextId);
        res.put("event", event);
        return res;
    }



    public static ClientInput fromJSON(JSONObject jsonObject){
        return new ClientInput(jsonObject.getString("fsmContextId"), jsonObject.getString("event"));
    }
}
