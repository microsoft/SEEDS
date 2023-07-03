package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.fsm.actions.ActionGenerator;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.SkipAction;
import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Enumeration;
import java.util.List;

public class FSMGeneratorAPI {

    protected static JSONObject getSingleStringStateData(String str){
        JSONObject res = new JSONObject();
        res.put(Constants.STATE_DATA_TEXT_KEY, str);
        return res;
    }
    protected static void createState(FSMAction entryAction, int stateID, JSONObject data, FSMAction exitAction, ExpectedInputType expectedInputType, ExpFSM fsm)
    {
        try {
            if(entryAction != null && exitAction != null && expectedInputType != null)
            {
                fsm.addState(entryAction, stateID, data, exitAction, expectedInputType);
            }
            else if(entryAction != null && exitAction != null)
            {
                fsm.addState(entryAction, stateID, data, exitAction);
            }
            else if(entryAction != null)
            {
                fsm.addState(entryAction,stateID,data);
            }
            else if(exitAction != null)
            {
                fsm.addState(stateID, data, exitAction);
            }
            else
            {
                fsm.addState(stateID, data);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    protected static void createEvent(String eventName, List<AudioFileWithSpeechRates> audioData, FSMAction defaultAction, ExpFSM fsm)
    {
        try {
            fsm.addEvent(eventName, audioData, defaultAction);
        } catch(Exception e) { e.printStackTrace(); }
    }

    protected static void createEvent(Event event, ExpFSM fsm){
        try{
            fsm.addEvent(event.name, event.audioData, event.defaultAction);
        }catch (Exception e) { e.printStackTrace(); }
    }

    protected static void createOpsEvent(String eventName, List<AudioFileWithSpeechRates> audioData, FSMAction action, ExpFSM fsm)
    {
        try {
            fsm.addOpsEvent(eventName, audioData, action);
        } catch(Exception e) { e.printStackTrace(); }
    }

    protected static void createOpsEvent(Event event, ExpFSM fsm)
    {
        try {
            fsm.addOpsEvent(event.name, event.audioData, event.defaultAction);
        } catch(Exception e) { e.printStackTrace(); }
    }

    protected static void createTransition(int sourceState, int targetState, String event, FSMAction action, ExpFSM fsm)
    {
        try {
            if(action == null)
                fsm.addTransition(sourceState,
                        event,
                        targetState,
                        new SkipAction());
            else
                fsm.addTransition(sourceState,
                        event,
                        targetState,
                        action);
        } catch(Exception e) { e.printStackTrace(); }
    }

    protected static void createFSMTransition(String event, ExpFSM sourceFSM, int sourceState,
                                            String targetFSMId, String targetFSMType, FSMAction action){
        try {
            sourceFSM.addFSMTransition(sourceState, event, targetFSMId, targetFSMType, action);
        }catch(Exception e) { e.printStackTrace(); }
    }

    public static JSONObject serialiseFSM(ExpFSM expFSM){
        JSONObject res = new JSONObject();
        JSONArray statesJSONArray = new JSONArray();
        JSONArray transitionJSONArray =  new JSONArray();
        JSONArray eventsJSONArray = new JSONArray();
        JSONArray opsEventsJSONArray = new JSONArray();
        Enumeration<Integer> statesEnum = expFSM.states.keys();
        while (statesEnum.hasMoreElements()) {
            int key = statesEnum.nextElement();
            State currState = (State)expFSM.states.get(key);
            statesJSONArray.put(currState.toJSON());
            currState.getTransitionsAndFSMTransitionsJSONArray().forEach(transitionJSONArray::put);
        }
        for(Event event : expFSM.getEvents()){
            eventsJSONArray.put(event.toJSON());
        }
        for(Event event : expFSM.getOpsEvents()){
            opsEventsJSONArray.put(event.toJSON());
        }
        res.put("_id", expFSM.getId());
        res.put("type", expFSM.getType());
        res.put("version", expFSM.getVersion());
        res.put("initAction", expFSM.initaction.getInstanceArgs());
        res.put("firstState", expFSM.first.getId());
        res.put("states", statesJSONArray);
        res.put("transitions", transitionJSONArray);
        res.put("events", eventsJSONArray);
        res.put("opsEvents", opsEventsJSONArray);
        res.put("language", expFSM.getLanguage());
        return res;
    }

    public static ExpFSM deserializeFSM(JSONObject jsonObject){
        ExpFSM fsm = new ExpFSM(jsonObject.getString("_id"), jsonObject.getInt("version"),
                jsonObject.getString("type"), jsonObject.getString("language"));
        FSMAction initAction = ActionGenerator.getAction(jsonObject.getJSONArray("initAction"));
        fsm.setInitAction(initAction);
        JSONArray statesJSONArray = jsonObject.getJSONArray("states");
        for(int  i =0; i<statesJSONArray.length(); ++i){
            JSONObject stateJSON = statesJSONArray.getJSONObject(i);
            FSMAction stateEntry = ActionGenerator.getAction(stateJSON.getJSONObject("action").getJSONArray("onEntry"));
            FSMAction stateExit = ActionGenerator.getAction(stateJSON.getJSONObject("action").getJSONArray("onExit"));
            JSONObject data = null;
            if(stateJSON.has("data")){
                data = stateJSON.getJSONObject("data");
            }
            createState(stateEntry, stateJSON.getInt("id"), data, stateExit, ExpectedInputType.valueOf(stateJSON.getString("expectedInputType")), fsm);
        }
        fsm.setFirstState(jsonObject.getInt("firstState"));

        JSONArray eventsJSONArray = jsonObject.getJSONArray("events");
        for(int i = 0; i<eventsJSONArray.length(); ++i){
            JSONObject eventJSON = eventsJSONArray.getJSONObject(i);
            createEvent(Event.fromJSON(eventJSON), fsm);
        }

        JSONArray opsEventsJSONArray = jsonObject.getJSONArray("opsEvents");
        for(int i = 0; i<opsEventsJSONArray.length(); ++i){
            JSONObject eventJSON = opsEventsJSONArray.getJSONObject(i);
            createOpsEvent(Event.fromJSON(eventJSON), fsm);
        }

        JSONArray transitionsJSONArray = jsonObject.getJSONArray("transitions");
        for(int  i =0; i<transitionsJSONArray.length(); ++i){
            JSONObject json = transitionsJSONArray.getJSONObject(i);
            FSMAction action = ActionGenerator.getAction(json.getJSONArray("action"));
            switch (json.getString(State.STATE_TRANSITION_TYPE_KEY)){
                case State.FSM_TRANSTITION_VALUE:
                    createFSMTransition(json.getString("event"),
                            fsm,
                            json.getInt("source"),
                            json.getString("targetFSMId"),
                            json.getString("targetFSMType"),
                            action);
                    break;
                default:
                    createTransition(json.getInt("source"), json.getInt("target"),
                            json.getString("event"), action, fsm);
                    break;
            }

        }
        return fsm;
    }
}
