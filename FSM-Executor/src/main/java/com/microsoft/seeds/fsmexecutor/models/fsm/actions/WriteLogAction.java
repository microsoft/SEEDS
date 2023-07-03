package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class WriteLogAction implements FSMAction
{
    Logger logger = Logger.getLogger(WriteLogAction.class.getName());
    String message = null;

    public WriteLogAction() {
        setMessage("WRITE LOG ACTION DEFAULT LOG");
    }

    public WriteLogAction(String s)
    {
        setMessage(s);
    }

    public void setMessage(String s) { message = s; }
    public String getMessage() { return message; }

    public void execute(FSMContext fsmc, Object data)
    {
        if(getMessage() != null)
            logger.info(getMessage());
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<String>(Arrays.asList("WriteLogAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new WriteLogAction();
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

