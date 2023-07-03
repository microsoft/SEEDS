package com.microsoft.seeds.place.models.fsm.actions;


import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IncrementVariableAction implements FSMAction {
    private String key = "default";
    private int incrementBy = 1;

    public IncrementVariableAction setIncrementBy(int incrementBy) {
        this.incrementBy = incrementBy;
        return this;
    }

    public IncrementVariableAction setVariableName(String variableName){
        this.key = variableName;
        return this;
    }

    @Override
    public void execute(FSMContext fsmc, Object data) {
        if(fsmc.containsKey(key)){
            int curr = (int) fsmc.get(key);
            fsmc.put(key, curr+incrementBy);
        }else{
            fsmc.put(key, incrementBy);
        }
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("IncrementVariableAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new IncrementVariableAction();
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        json.put(JSON_NAME_KEY, getActionName().get(0));
        array.put(json);
        return array;
    }
}
