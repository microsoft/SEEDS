package com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel;

import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import org.json.JSONObject;

import java.time.Clock;

public class PullModelNodeOption {
    private String id;
    private AudioFileWithSpeechRates audioData;

    public PullModelNodeOption(AudioFileWithSpeechRates audioData) {
        this.audioData = audioData;
    }

    public PullModelNodeOption(String id, AudioFileWithSpeechRates audioData) {
        this.id = id;
        this.audioData = audioData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AudioFileWithSpeechRates getAudioData() {
        return audioData;
    }

    public void setAudioData(AudioFileWithSpeechRates audioData) {
        this.audioData = audioData;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", getId());
        res.put("audioData", getAudioData().toJSON());
        return res;
    }

    public static PullModelNodeOption fromJSON(JSONObject jsonObject){
        if(isLeafNodeOption(jsonObject)){ // IF jsonObject is a leaf node
            return PullModelLeafNodeOption.fromJSON(jsonObject);
        }
        return new PullModelNodeOption(
                jsonObject.getString("id"),
                AudioFileWithSpeechRates.fromJSON(jsonObject.getJSONObject("audioData")));
    }

    public String getGraphString(){
        return id;
    }

    public static boolean isLeafNodeOption(JSONObject jsonObject){
        return jsonObject.has("fsmType");
    }

    public static boolean isLeafNodeOption(PullModelNodeOption pullModelNodeOption){
        return pullModelNodeOption.toJSON().has("fsmType");
    }

    public static PullModelNodeOption getDummy(String optionName, boolean isLeaf){
        Clock clock = Clock.systemDefaultZone();
        if(isLeaf) {
            return new PullModelLeafNodeOption(optionName, Constants.FSM_TYPE_ON_DEMAND, AudioFileWithSpeechRates.getFor("dummyContainer/dummyBase/leaf/" + optionName), clock.millis());
        }
        return new PullModelNodeOption(optionName, AudioFileWithSpeechRates.getFor("dummyContainer/dummyBase/" + optionName));
    }

}
