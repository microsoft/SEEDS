package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

public class PushFSMContextDataAction implements FSMAction {
    private final Logger logger = Logger.getLogger(PushFSMContextDataAction.class.getName());
    @Override
    public void execute(FSMContext fsmc, Object data) {
        JSONObject res = new JSONObject();
        Enumeration<String> e = fsmc.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            res.put(key, String.valueOf(fsmc.get(key)));
        }
        res.put("id", fsmc.getId());
        pushData(res);
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<String>(Arrays.asList("PushFSMContextDataAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new PushFSMContextDataAction();
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        json.put(JSON_NAME_KEY, getActionName().get(0));
        array.put(json);
        return array;
    }

    private void pushData(JSONObject res){
        logger.info("PUSHING FSM DATA" );
        logger.info(res.toString());
    }
}
