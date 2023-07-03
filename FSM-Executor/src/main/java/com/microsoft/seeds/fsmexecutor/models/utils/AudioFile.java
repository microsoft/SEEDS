package com.microsoft.seeds.fsmexecutor.models.utils;

import org.json.JSONObject;

import java.util.Map;

public class AudioFile {
    public String filePath;
    public long duration;
    public double speechRate;

    public AudioFile() {
        duration = 0;
    }
    public AudioFile(String filePath, long duration, double speechRate) {
        this.filePath =  filePath;
        this.duration = duration;
        this.speechRate = speechRate;
    }
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(double speechRate) {
        this.speechRate = speechRate;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("filePath", filePath);
        res.put("duration", duration);
        res.put("speechRate", speechRate);
        return res;
    }

    public JSONObject getJSONWithoutSpeechRate(){
        JSONObject thisObj = this.toJSON();
        thisObj.remove("speechRate");
        return thisObj;
    }

    public Map<String, Object> getMapWithoutSpeechRate(){
        return getJSONWithoutSpeechRate().toMap();
    }
    public static AudioFile getInstanceFromJSON(JSONObject json, double speechRate){
        return new AudioFile(json.getString("filePath"), json.getInt("duration"), speechRate);
    }
}
