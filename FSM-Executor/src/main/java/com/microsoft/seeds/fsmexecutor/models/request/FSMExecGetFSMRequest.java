package com.microsoft.seeds.fsmexecutor.models.request;

public class FSMExecGetFSMRequest {
    public String id;
    public String type;
    public String fsmType;

    public FSMExecGetFSMRequest(String id, String fsmType) {
        this.id = id;
        this.fsmType = fsmType;
    }

    public FSMExecGetFSMRequest(String id, String type, String fsmType) {
        this.id = id;
        this.type = type;
        this.fsmType = fsmType;
    }

    public FSMExecGetFSMRequest() {
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

    public String getFsmType() {
        return fsmType;
    }

    public void setFsmType(String fsmType) {
        this.fsmType = fsmType;
    }
}
