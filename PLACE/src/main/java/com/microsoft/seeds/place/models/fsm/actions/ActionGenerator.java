package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ActionGenerator {
    private static HashMap<String, FSMAction> fsmActionHashMap;
    static {
        fsmActionHashMap = new HashMap<>();
        List<FSMAction> fsmActionList = new ArrayList<>(Arrays.asList(
                new AutoEventDispatchAction(),
                new AutoPopFSMAction(),
                new DecrementVariableAction(),
                new ExecuteAfterWaitTimeAction(),
                new IncrementVariableAction(),
                new PauseAction(),
                new PushFSMContextDataAction(),
                new PushFSMEventsAction(),
                new PushFSMStateAction(),
                new RegisterFSMContextWithAutoDispatchThreadAction(),
                new ResumeAction(),
                new ScrambleMapLinesRandomlyAction(),
                new SkipAction(),
                new StdOutAction(),
                new StopAction(),
                new TimeoutEventDispatchAction(),
                new WriteLogAction()
        ));
        fsmActionList.forEach(fsmActionObj -> fsmActionHashMap.put(fsmActionObj.getActionName().get(0), fsmActionObj));
    }
    public static FSMAction getAction(JSONArray actions){
        FSMActionList res = new FSMActionList();
        for (Object item : actions) {
            JSONObject jsonObject = (JSONObject) item;
            String name = jsonObject.getString(FSMAction.JSON_NAME_KEY);
            if (fsmActionHashMap.containsKey(name)) {
                if(jsonObject.has(FSMAction.JSON_ARGS_KEY)) {
                    res = res.add(fsmActionHashMap.get(name)
                            .getInstanceFromArgs(jsonObject.getJSONObject(FSMAction.JSON_ARGS_KEY)));
                }else{
                    res = res.add(fsmActionHashMap.get(name)
                            .getInstanceFromArgs(null));
                }
            }
        }
        return res;
    }
}
