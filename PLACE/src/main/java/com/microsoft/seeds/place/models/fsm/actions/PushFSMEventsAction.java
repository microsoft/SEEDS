package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PushFSMEventsAction implements FSMAction {
    private static final Logger logger = LoggerFactory.getLogger(PushFSMEventsAction.class.getName());
    @Override
    public void execute(FSMContext fsmc, Object data) {
        logger.info("PUSHING EVENT LIST : " + fsmc.getEventNameList());
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Arrays.asList("PushFSMEventsAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new PushFSMEventsAction();
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
