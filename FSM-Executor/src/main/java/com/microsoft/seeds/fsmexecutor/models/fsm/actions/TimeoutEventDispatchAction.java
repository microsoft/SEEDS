package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeoutEventDispatchAction implements FSMAction {
    @Override
    public void execute(FSMContext fsmc, Object data) {
        fsmc.dispatch(Constants.TIMEOUT_EVENT, data);
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("TimeoutEventDispatchAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new TimeoutEventDispatchAction();
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
