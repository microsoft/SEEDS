package com.microsoft.seeds.place.models.fsm;

import com.microsoft.seeds.place.models.fsm.actions.ActionGenerator;
import com.microsoft.seeds.place.models.fsm.actions.SkipAction;
import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.place.models.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    public String name;
    public List<AudioFileWithSpeechRates> audioData;
    public FSMAction defaultAction;

    public Event(String name, List<AudioFileWithSpeechRates> audioData) {
        this.name = name;
        this.audioData  = audioData == null ? new ArrayList<>() : audioData;
        defaultAction = new SkipAction();
    }

    public Event(String name, List<AudioFileWithSpeechRates> audioData, FSMAction defaultAction) {
        this.name = name;
        this.audioData  = audioData == null ? new ArrayList<>() : audioData;
        this.defaultAction = defaultAction;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("name", name);
        res.put(Constants.AUDIO_DATA_KEY, getAudioDataJSONArray());
        if(defaultAction != null)
            res.put("defaultAction", defaultAction.getInstanceArgs());
        return res;
    }
    public JSONArray getAudioDataJSONArray(){
        JSONArray res = new JSONArray();
        for (AudioFileWithSpeechRates audioFileWithSpeechRates : audioData) {
            res.put(audioFileWithSpeechRates.toJSON());
        }
        return res;
    }

    private List<Map<String, Object>> getAudioDataMap(){
        List<Map<String, Object>> res = new ArrayList<>();
        for (AudioFileWithSpeechRates audioFileWithSpeechRates : audioData) {
            res.add(audioFileWithSpeechRates.toMap());
        }
        return res;
    }

    public Map<String, Object> getClientStateEventObj(){
        Map<String, Object> res = new HashMap<>();
        res.put("name", name);
        res.put(Constants.AUDIO_DATA_KEY, getAudioDataMap());
        return res;
    }

    public static Event fromJSON(JSONObject jsonObject){
        FSMAction action = null;
        if(jsonObject.has("defaultAction")){
            action = ActionGenerator.getAction(jsonObject.getJSONArray("defaultAction"));
        }
        JSONArray audioDataJSONArray = jsonObject.getJSONArray(Constants.AUDIO_DATA_KEY);
        List<AudioFileWithSpeechRates> audioData = new ArrayList<>();
        for(int i = 0; i<audioDataJSONArray.length(); ++i){
            JSONObject audioDataJSONItem = audioDataJSONArray.getJSONObject(i);
            audioData.add(AudioFileWithSpeechRates.fromJSON(audioDataJSONItem));
        }
        return new Event(jsonObject.getString("name"), audioData, action);
    }
}
