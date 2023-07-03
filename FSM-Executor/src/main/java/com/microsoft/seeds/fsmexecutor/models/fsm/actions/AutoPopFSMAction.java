package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.PullModelFSMContextController;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoPopFSMAction implements FSMAction {
    @Override
    public void execute(FSMContext fsmc, Object data) {
//        if(fsmc.getComposableFSMContextExecutor() != null){
//            fsmc.getComposableFSMContextExecutor().dispatch(ComposableFSMContextExecutor.POP_FSM_EVENT, null);
//        }
        if(fsmc.getFsmContextController().canPopFSM()) {
            fsmc.getFsmContextController().dispatch(PullModelFSMContextController.POP_FSM_EVENT, null, fsmc);
        }
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Arrays.asList("AutoPopFSMAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new AutoPopFSMAction();
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
