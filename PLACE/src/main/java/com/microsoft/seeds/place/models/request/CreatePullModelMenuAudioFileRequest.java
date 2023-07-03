package com.microsoft.seeds.place.models.request;

import net.minidev.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class CreatePullModelMenuAudioFileRequest {
    public String basePath;
    public String type;
    public HashMap<String, Object> filePaths;
    public List<String> speechRates;

    public CreatePullModelMenuAudioFileRequest() {
    }

    public List<String> getSpeechRates() {
        return speechRates;
    }

    public void setSpeechRates(List<String> speechRates) {
        this.speechRates = speechRates;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HashMap<String, Object> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(HashMap<String, Object> filePaths) {
        this.filePaths = filePaths;
    }

    public static HashMap<String, Object> getMap(JSONObject json){
        HashMap<String, Object> res = new HashMap<>();
        Iterator<String> keys = json.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            res.put(key, json.get(key));
        }
        return res;
    }

    public static CreatePullModelMenuAudioFileRequest fromJSON(JSONObject json){
        CreatePullModelMenuAudioFileRequest res = new CreatePullModelMenuAudioFileRequest();
        List<String> speechRates = new ArrayList<>();
        if(json.has("speechRates")) {
            speechRates = json.getJSONArray("speechRates")
                    .toList()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        res.setBasePath(json.getString("basePath"));
        res.setType(json.getString("type"));
        res.setFilePaths(getMap(json.getJSONObject("filePaths")));
        res.setSpeechRates(speechRates);
        return res;
    }

    public JSONObject getUpdatedPullModelMenuData(JSONObject data, int i){
        String[] parts = basePath.split("/");
        if(i == parts.length){
            for(Map.Entry<String, Object> entry : filePaths.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
            return data;
        }
        if(!data.has(parts[i])){
            data.put(parts[i], new JSONObject());
        }
        data.put(parts[i], getUpdatedPullModelMenuData(data.getJSONObject(parts[i]), i+1));
        return data;
    }
}
