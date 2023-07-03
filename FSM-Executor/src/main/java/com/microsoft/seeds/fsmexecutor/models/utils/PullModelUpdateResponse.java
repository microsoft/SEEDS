package com.microsoft.seeds.fsmexecutor.models.utils;

// IMMUTABLE RESPONSE CLASS
public class PullModelUpdateResponse {
    private String message;
    private int status;

    public PullModelUpdateResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
