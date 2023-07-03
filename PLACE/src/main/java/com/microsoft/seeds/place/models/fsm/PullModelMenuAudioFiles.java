package com.microsoft.seeds.place.models.fsm;

import org.json.JSONObject;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PullModelMenuAudioFiles {
    @Id
    int id;
    public JSONObject data;
    public List<String> speechRates;

    public PullModelMenuAudioFiles() {
        data = new JSONObject();
        speechRates = new ArrayList<>();
    }

    public PullModelMenuAudioFiles(int id, JSONObject data, List<String> speechRates) {
        this.id = id;
        this.data = data;
        this.speechRates = speechRates;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public List<String> getSpeechRates() {
        return speechRates;
    }

    public void setSpeechRates(List<String> speechRates) {
        this.speechRates = speechRates;
    }

    public JSONObject serialize(){
        JSONObject res = new JSONObject();
        res.put("_id", id);
        res.put("data", data);
        res.put("speechRates", speechRates);
        return res;
    }

    public static PullModelMenuAudioFiles deserialize(JSONObject jsonObject){
        List<String> speechRates = new ArrayList<>();
        if(jsonObject.has("speechRates")) {
            speechRates = jsonObject.getJSONArray("speechRates")
                    .toList()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return new PullModelMenuAudioFiles(jsonObject.getInt("_id"), jsonObject.getJSONObject("data"), speechRates);
    }

}
