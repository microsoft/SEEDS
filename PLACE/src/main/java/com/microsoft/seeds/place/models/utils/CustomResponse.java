package com.microsoft.seeds.place.models.utils;

import org.json.JSONObject;

import java.util.HashMap;

public class CustomResponse {
    public static HashMap<String, Object> getSuccessResponse(Object... msg){
        HashMap<String, Object> res = new HashMap<String, Object>(){{
            put("status", "success");
        }};
        if(msg.length > 0){
            for(int i = 0; i<msg.length - 1; i=i+2){
                res.put(msg[i].toString(), msg[i+1]);
            }
        }
        return res;

    }
    public static HashMap<String, Object> getErrorResponse(String errorMessage){
        return new HashMap<String, Object>(){{
            put("status", "error");
            put("message", errorMessage);
        }};
    }

    public static HashMap<String, Object> getObjResponse(JSONObject jsonObject){
        return (HashMap<String, Object>) jsonObject.toMap();
    }
}
