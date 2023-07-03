package com.microsoft.seeds.place.models.fsm;

import com.microsoft.seeds.place.models.fsm.generators.QuizGenerator;
import com.microsoft.seeds.place.models.utils.Constants;
import org.apache.tomcat.util.bcel.Const;
import org.json.JSONObject;

public class OnDemandFSMData {
    public String id;
    public String type;
    public JSONObject data;
    public int version;
    public long timeStamp;

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public OnDemandFSMData() {
    }

    public OnDemandFSMData(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public OnDemandFSMData(String id, String type, JSONObject data, int version, long timeStamp) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.version = version;
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

    public JSONObject serialize(){
        JSONObject res = new JSONObject();
        res.put("_id", id);
        res.put("type", type);
        res.put("version", version);
        res.put(Constants.FSM_TYPE_KEY, Constants.FSM_TYPE_ON_DEMAND);
        res.put("data", data);
        res.put("timeStamp", timeStamp);
        return res;
    }

    public static OnDemandFSMData deserialize(JSONObject json){
        OnDemandFSMData res = new OnDemandFSMData(json.getString("_id"), json.getString("type"));
        res.setVersion(json.getInt("version"));
        res.setData(json.getJSONObject("data"));
        res.setTimeStamp(json.getLong("timeStamp"));
        return res;
    }

    public ExpFSM getFSM(){
        ExpFSM fsm;
        switch (this.type){
            default:
                fsm = QuizGenerator.getFSMFromOnDemandFSMData(this);
        }
        return fsm;
    }
}
