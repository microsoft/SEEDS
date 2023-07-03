package com.microsoft.seeds.fsmexecutor.models.fsm.actions;


import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecrementVariableAction implements FSMAction {
    private String key = "default";
    private int decrementBy = 0;

    public DecrementVariableAction setDecrementBy(int decrementBy) {
        this.decrementBy = decrementBy;
        return this;
    }

    public DecrementVariableAction setVariableName(String variableName){
        this.key = variableName;
        return this;
    }

    @Override
    public void execute(FSMContext fsmc, Object data) {
        if(fsmc.containsKey(key)){
            int curr = (int) fsmc.get(key);
            fsmc.put(key, curr - decrementBy);
        }else{
            fsmc.put(key, -1 * decrementBy);
        }
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Arrays.asList("DecrementVariableAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new DecrementVariableAction();
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
