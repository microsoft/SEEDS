package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.request.CreateQuizRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RawData {
    public String id;
    public String type;
    public JSONObject data;
    public boolean isProcessed;
    public long timeStamp;

    public RawData() {
    }

    public RawData(String id, String type, JSONObject data, boolean isProcessed){
        this.id = id;
        this.data = data;
        this.type = type;
        this.isProcessed = isProcessed;
    }

    public RawData(String id, String type, JSONObject data, boolean isProcessed, long timeStamp) {
        this.id = id;
        this.data = data;
        this.type = type;
        this.isProcessed = isProcessed;
        this.timeStamp = timeStamp;
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

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public static RawData fromJSON(JSONObject json){
        return new RawData(json.getString("_id"), json.getString("type"), json.getJSONObject("data"), json.getBoolean("isProcessed"), json.getLong("timeStamp"));
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("_id", id);
        res.put("data", data);
        res.put("type", type);
        res.put("isProcessed", isProcessed);
        res.put("timeStamp", timeStamp);
        return res;
    }

    public RawData processBasedOnType(){
        switch (type){
            case Constants.QUIZ_FSM_TYPE:
                CreateQuizRequest createQuizRequest  = CreateQuizRequest.fromJSON(data);
                List<List<String>> fullOptions = new ArrayList<>();
                for(int questionNum = 0; questionNum < createQuizRequest.questions.size(); ++questionNum) {
                    List<String> options = data.getJSONArray("options")
                            .getJSONArray(questionNum)
                            .toList()
                            .stream()
                            .map(item -> String.valueOf(item))
                            .collect(Collectors.toList());
                    int correctAnswer = createQuizRequest.correctAnswers.get(questionNum);
                    if(correctAnswer != 0){
                        Collections.swap(options, 0, correctAnswer);
                    }
                    fullOptions.add(options);
                }
                data.put("options", fullOptions);
                data.remove("correctAnswers");
                return this;
            default:
                return this;
        }
    }
}
