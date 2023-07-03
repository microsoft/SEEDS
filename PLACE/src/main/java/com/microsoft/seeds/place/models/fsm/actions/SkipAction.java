package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkipAction implements FSMAction
{
    public void execute(FSMContext fsmc, Object data)
    { // by default do nothing :)
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<String>(Arrays.asList("SkipAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new SkipAction();
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

