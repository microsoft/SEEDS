package com.microsoft.seeds.place.models.fsm.pullmodel;

import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PullModelNode {
    private String id;
    private String parentId;
    private List<AudioFileWithSpeechRates> audioFileWithSpeechRatesList;
    private List<PullModelNodeOption> options;
    private boolean isLeaf;

    public PullModelNode(String id, String parentId, List<PullModelNodeOption> options,
                         List<AudioFileWithSpeechRates> audioFileWithSpeechRatesList) {
        this.id = id;
        this.parentId = parentId;
        this.options = options;
        this.audioFileWithSpeechRatesList = audioFileWithSpeechRatesList;
        this.isLeaf = false;
    }

    public PullModelNode(String id, String parentId, List<PullModelNodeOption> options,
                         List<AudioFileWithSpeechRates> audioFileWithSpeechRatesList, boolean isLeaf) {
        this.id = id;
        this.parentId = parentId;
        this.options = options;
        this.audioFileWithSpeechRatesList = audioFileWithSpeechRatesList;
        this.isLeaf = isLeaf;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<AudioFileWithSpeechRates> getAudioFileWithSpeechRatesList() {
        return audioFileWithSpeechRatesList;
    }

    public void setAudioFileWithSpeechRatesList(List<AudioFileWithSpeechRates> audioFileWithSpeechRatesList) {
        this.audioFileWithSpeechRatesList = audioFileWithSpeechRatesList;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<PullModelNodeOption> getOptions() {
        return options;
    }

    public void setOptions(List<PullModelNodeOption> options) {
        this.options = options;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("parentId", parentId);
        res.put("isLeaf", isLeaf);
        res.put("options", options.stream().map(PullModelNodeOption::toJSON).collect(Collectors.toList()));
        res.put("audioData", audioFileWithSpeechRatesList.stream()
                .map(AudioFileWithSpeechRates::toJSON)
                .collect(Collectors.toList()));
        return res;
    }

    public static PullModelNode fromJSON(JSONObject jsonObject){
        return new PullModelNode(
                jsonObject.getString("id"),
                jsonObject.getString("parentId"),
                jsonObject.getJSONArray("options")
                        .toList()
                        .stream().map(option -> PullModelNodeOption.fromJSON(new JSONObject((HashMap)option)))
                        .collect(Collectors.toList()),
                jsonObject.getJSONArray("audioData")
                        .toList()
                        .stream()
                        .map(audioFile -> AudioFileWithSpeechRates.fromJSON(new JSONObject((HashMap) audioFile)))
                        .collect(Collectors.toList()),
                jsonObject.getBoolean("isLeaf")
        );
    }

    public void addOption(PullModelNodeOption option){
        this.options.add(option);
    }

    public void addOption(PullModelNodeOption option, int index){
        this.options.add(index, option);
    }
}
