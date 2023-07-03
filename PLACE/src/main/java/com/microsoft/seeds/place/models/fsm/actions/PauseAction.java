package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.AutoStateDispatchThread;
import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PauseAction implements FSMAction {
    @Override
    public void execute(FSMContext fsmc, Object data) {
        AutoStateDispatchThread.getInstance().pauseExecution(fsmc.getId());
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("PauseAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new PauseAction();
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
