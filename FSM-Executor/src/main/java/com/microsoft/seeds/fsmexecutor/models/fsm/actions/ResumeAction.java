package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.AutoStateDispatchThread;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResumeAction implements FSMAction {
    @Override
    public void execute(FSMContext fsmc, Object data) {
        AutoStateDispatchThread.getInstance().resumeExecuting(fsmc.getId());
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Arrays.asList("ResumeAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new ResumeAction();
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
