package com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel;

import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import org.json.JSONObject;

public class PullModelLeafNodeOption extends PullModelNodeOption {
    private String fsmType;
    private long creationTimestamp;
    private String title;
    public PullModelLeafNodeOption(String id, String fsmType, AudioFileWithSpeechRates audioFile, long creationTimestamp) {
       super(id, audioFile);
        this.fsmType = fsmType;
        this.creationTimestamp = creationTimestamp;
        this.title = id;
    }

    public PullModelLeafNodeOption(String id, String fsmType, String title, AudioFileWithSpeechRates audioFile, long creationTimestamp) {
        super(id, audioFile);
        this.fsmType = fsmType;
        this.creationTimestamp = creationTimestamp;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFsmType() {
        return fsmType;
    }

    public void setFsmType(String fsmType) {
        this.fsmType = fsmType;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", getId());
        res.put("fsmType", fsmType);
        res.put("audioData", getAudioData().toJSON());
        res.put("creationTimestamp", creationTimestamp);
        res.put("title", title);
        return res;
    }

    @Override
    public String getGraphString(){
        return title;
    }

    public static PullModelLeafNodeOption fromJSON(JSONObject jsonObject){
        return new PullModelLeafNodeOption(
                jsonObject.getString("id"),
                jsonObject.getString("fsmType"),
                jsonObject.getString("title"),
                AudioFileWithSpeechRates.fromJSON(jsonObject.getJSONObject("audioData")),
                jsonObject.getLong("creationTimestamp"));
    }
}
