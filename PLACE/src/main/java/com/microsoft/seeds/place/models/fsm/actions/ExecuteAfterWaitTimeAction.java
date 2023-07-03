package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import com.microsoft.seeds.place.models.utils.OnCompleteValidation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecuteAfterWaitTimeAction implements FSMAction {
    private long waitTime;
    private FSMAction onCompleteAction;
    private OnCompleteValidation onCompleteValidation;
    private int onCompleteValidationStateId;

    public ExecuteAfterWaitTimeAction() {
    }

    public ExecuteAfterWaitTimeAction(long waitTime, int onCompleteValidationStateId, FSMAction onCompleteAction) {
        this.waitTime = waitTime;
        this.onCompleteAction = onCompleteAction;
        this.onCompleteValidationStateId = onCompleteValidationStateId;
        this.onCompleteValidation = getOnCompleteValidationWithStateId(onCompleteValidationStateId);
    }

    @Override
    public void execute(FSMContext fsmc, Object data) {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if(onCompleteValidation.check(fsmc)){
                            onCompleteAction.execute(fsmc, data);
                        }
                    }
                },
                waitTime
        );

    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("ExecuteAfterWaitTimeAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        FSMAction action = ActionGenerator.getAction(args.getJSONArray("onCompleteAction"));
        return new ExecuteAfterWaitTimeAction(args.getLong("waitTime"), args.getInt("onCompleteValidationStateId"), action);
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        JSONObject args = new JSONObject();
        args.put("waitTime", waitTime);
        args.put("onCompleteValidationStateId", onCompleteValidationStateId);
        args.put("onCompleteAction", onCompleteAction.getInstanceArgs());
        json.put(JSON_ARGS_KEY, args);
        json.put(JSON_NAME_KEY, getActionName().get(0));
        array.put(json);
        return array;
    }

    private static OnCompleteValidation getOnCompleteValidationWithStateId(int stateId){
        return (fsmc) -> {
            if(fsmc.getCurrState().getId() == stateId){
                return true;
            }
            return false;
        };
    }
}
