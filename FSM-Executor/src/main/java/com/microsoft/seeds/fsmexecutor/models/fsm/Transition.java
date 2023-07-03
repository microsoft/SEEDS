package com.microsoft.seeds.fsmexecutor.models.fsm;

import org.json.JSONObject;

/**
 * Transitions are created on the fly and should not be created manually.
 */
public class Transition implements java.io.Serializable
{
    private static final long serialVersionUID = -295422703855886286L;
    State target;
    State source;

    FSMAction action;

    public Transition(State s, State t, FSMAction a)
    {
        source = s;
        target = t;
        action = a;
    }

    public void execute(FSMContext fsmc, Object data)
    {
        // trigger a state exit event in the old state
        // execute the action
        source.getStateExitAction().execute(fsmc, data);
        if(action != null)
            action.execute(fsmc, data);
        fsmc.setCurrState(target);
        target.getStateEntryAction().execute(fsmc, data);
    }

    public JSONObject toJSON(String eventName){
        JSONObject res = new JSONObject();
        res.put("event", eventName);
        res.put("source", source.getId());
        res.put("target", target.getId());
        res.put("action", action.getInstanceArgs());
        return res;
    }
}

