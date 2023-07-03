package com.microsoft.seeds.place.models.fsm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * The FSM uses the command pattern to implement actions. All actions must
 * implement this interface.
 */
public interface FSMAction extends java.io.Serializable
{
    /**
     * @param fsmc This is the context in which the command is executed. The
     * context can be used as a repository for objects. That is because
     * FSMContext extends from java.util.Hashtable.
     * @param data Some extra data that can be given to a command
     */
    static final String JSON_NAME_KEY = "name";
    static final String JSON_ARGS_KEY = "args";
    static final String JSON_ACTIONS_KEY = "actions";
    static final long serialVersionUID = 1L;
    public void execute(FSMContext fsmc, Object data);
    public List<String> getActionName();
    public FSMAction getInstanceFromArgs(JSONObject args);
    public JSONArray getInstanceArgs();
}

