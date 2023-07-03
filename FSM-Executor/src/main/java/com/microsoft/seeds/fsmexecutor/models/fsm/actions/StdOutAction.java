package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class StdOutAction implements FSMAction {
    Logger logger = Logger.getLogger(WriteLogAction.class.getName());
    @Override
    public void execute(FSMContext fsmc, Object data) {
        if(fsmc.getCurrState().getData() != null && fsmc.getCurrState().getData().has("text")){
            logger.info((String) fsmc.getCurrState().getData().get("text"));
        }
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Arrays.asList("StdOutAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new StdOutAction();
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
