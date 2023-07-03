package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.fsm.actions.SkipAction;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * This class models a State. A state has an ID and entry /exit actions.
 * Further state also contains a dispatch mechanism for incoming events.
 */
public class State implements java.io.Serializable
{
    private static final long serialVersionUID = -295422703255886286L;
    public static final String STATE_TRANSITION_TYPE_KEY = "type";
    public static final String TRANSITION_VALUE = "transition";
    public static final String FSM_TRANSTITION_VALUE = "fsmTransition";
    private int id;
    private Hashtable<String, Transition> transitions = new Hashtable();
    private Hashtable<String, FSMTransition> fsmTransitions = new Hashtable<>();
    private List<String> events = new ArrayList<>();
    private JSONObject data;

    private ExpectedInputType expectedInputType;

    public static SkipAction skip = new SkipAction();

    FSMAction onStateEntry = skip;
    FSMAction onStateExit = skip;

    /**
     * Initilizes a stateobject with name s.
     * @param id The ID of the new state
     */
    public State(int id, JSONObject data) {
        setId(id);
        setData(data);
        setExpectedInputType(ExpectedInputType.EVENT_NAME);
    }

    public State(int id, JSONObject data, ExpectedInputType expectedInputType) {
        setId(id);
        setData(data);
        setExpectedInputType(expectedInputType);
    }

    public void setId(int s) { id = s; }
    public int getId() { return id; }

    public ExpectedInputType getExpectedInputType() {
        return expectedInputType;
    }

    public void setExpectedInputType(ExpectedInputType expectedInputType) {
        this.expectedInputType = expectedInputType;
    }

    public JSONObject getData() {return data;}
    public void setData(JSONObject data){this.data = data;}

    public JSONArray getTransitionsAndFSMTransitionsJSONArray(){
        JSONArray res = new JSONArray();
        this.events.forEach(eventName -> {
            if(transitions.containsKey(eventName)){
                JSONObject json = transitions.get(eventName).toJSON(eventName);
                json.put(STATE_TRANSITION_TYPE_KEY, TRANSITION_VALUE);
                res.put(json);
            }else if(fsmTransitions.containsKey(eventName)){
                JSONObject json = fsmTransitions.get(eventName).toJSON(eventName);
                json.put(STATE_TRANSITION_TYPE_KEY, FSM_TRANSTITION_VALUE);
                res.put(json);
            }
        });
        return res;
    }

    public void setStateEntryAction(FSMAction action)
    {
        onStateEntry = action;
    }
    public FSMAction getStateEntryAction()
    {
        return onStateEntry;
    }

    public void setStateExitAction(FSMAction action)
    {
        onStateExit = action;
    }

    public FSMAction getStateExitAction()
    {
        return onStateExit;
    }

    /**
     * Convenience method that returns the ID of this state.
     */
    public String toString()
    {
        return String.valueOf(getId());
    }

    /**
     * Adds a transition with this state as source and parameter to as a
     * target.
     * @param trigger The event that triggers the transition
     * @param to The target state.
     * @param action The associated action
     */
    public void addTransition(String trigger, State to, FSMAction action)
    {
        events.removeIf( (event) -> event.equals(trigger) );
        events.add(trigger);
        transitions.put(trigger, new Transition(this,to,action));
    }

    public void addFSMTransition(String trigger, String targetFSMId, String targetFSMType, FSMAction action){
        events.removeIf( (event) -> event.equals(trigger) );
        events.add(trigger);
        fsmTransitions.put(trigger, new FSMTransition(this, targetFSMId, targetFSMType, action));
    }

    /**
     * Dispatch an event.
     * @param event The event that needs to be dispatched. The correct
     * transition is located and than executed.
     * @param data Some additional data that may be needed by the action
     * @param fsmc The context in which the action is executed. This may be
     * useful for retrieving global variables.
     */
    public void dispatch(Event event, Object data, FSMContext fsmc)
    {
        if(transitions.containsKey(event.name)){
            transitions.get(event.name).execute(fsmc, data);
        }else if(fsmTransitions.containsKey(event.name)){
            fsmTransitions.get(event.name).execute(fsmc, null);
        }
        else if(event.defaultAction != null){
            event.defaultAction.execute(fsmc, data);
        }
    }

    /**
     * Method to find out which events can be dispatched by this state.
     * @return A vector with the events
     */
    public List<String> getEventNames()
    {
        List<String> filteredEvents = events;
        filteredEvents.remove(Constants.AUTO_EVENT);
        filteredEvents.remove(Constants.TIMEOUT_EVENT);
        return filteredEvents;
    }

    public boolean isEndState(){
        return transitions.isEmpty();
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        JSONObject actions = new JSONObject();
        actions.put("onEntry", onStateEntry.getInstanceArgs());
        actions.put("onExit", onStateExit.getInstanceArgs());
        res.put("action", actions);
        res.put("data", data);
        res.put("expectedInputType", expectedInputType.name());
        return res;
    }
    public ClientState getClientState(){
        return new ClientState(id, data == null ? new HashMap<>() : data.toMap(), isEndState(), expectedInputType);
    }
}

