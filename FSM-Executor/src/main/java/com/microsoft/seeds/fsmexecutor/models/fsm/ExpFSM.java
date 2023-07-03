package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.fsm.actions.SkipAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.FSMContextController;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.PullModelFSMContextController;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.ScrambleFSMContextController;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.StaticFSMContextController;
import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class serves as an access point for the whole framework. An FSM object
 * encapsulates an FSM and provides a factory method to create a FSMContext
 * object for this FSM.
 */
public class ExpFSM implements java.io.Serializable
{

    private String id;
    private int version;
    private String type;
    private String language;
    Hashtable states = new Hashtable();
    public Hashtable events = new Hashtable();
    Hashtable opsEvents = new Hashtable();
    State first = null;

    FSMAction initaction = null;

    FSMContextController fsmContextController;

    public ExpFSM(String id, int version, String type, String language) {
        this.id = id;
        this.type = type;
        this.language = language;
        this.version = version;
        setInitAction(new SkipAction());
        switch (type){
            case Constants.SCRAMBLE_FSM_TYPE:
                fsmContextController = new ScrambleFSMContextController(this);
                break;
            case Constants.PULL_MODEL_FSM_TYPE:
                fsmContextController = new PullModelFSMContextController(this);
                break;
            default:
                fsmContextController = new StaticFSMContextController(this);
                break;
        }
    }

    public String getId() {
        return id;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * This method can be used to add a state to the FSM.
     * FSM uses the State class to create a new State object
     * @param stateId The ID of the state
     * @param data The JSONObject data stored in the state
     */
    public void addState(int stateId, JSONObject data)
    {
        if(!states.containsKey(stateId))
        {
            State s = new State(stateId, data);
            states.put(stateId, s);
        }
        else throw new RuntimeException("state; " + stateId +
                " already declared");
    }

    /**
     * This method can be used to add a state to the FSM.
     * FSM uses the State class to create a new State object
     * @param entryAction The action that is executed upon state-entry
     * @param stateId The ID of the state
     * @param data The JSONObject data stored in the state
     */
    public void addState(FSMAction entryAction, int stateId, JSONObject data)
    {
        if(!states.containsKey(stateId))
        {
            State s = new State(stateId, data);
            s.setStateEntryAction(entryAction);
            states.put(stateId, s);
        }
        else throw new RuntimeException("state; " + stateId +
                " already declared");
    }

    /**
     * This method can be used to add a state to the FSM.
     * FSM uses the State class to create a new State object
     * @param stateId The ID of the state
     * @param exitAction The action that is executed upon state-exit
     * @param data The JSONObject data stored in the state
     */
    public void addState(int stateId, JSONObject data, FSMAction exitAction)
    {
        if(!states.containsKey(stateId))
        {
            State s = new State(stateId, data);
            s.setStateExitAction(exitAction);
            states.put(stateId, s);
        }
        else throw new RuntimeException("state; " + stateId +
                " already declared");
    }

    /**
     * This method can be used to add a state to the FSM.
     * FSM uses the State class to create a new State object
     * @param entryAction The action that is executed upon state-entry
     * @param stateId The ID of the state
     * @param data The JSONObject data stored in the state
     * @param exitAction The action that is executed upon state-exit
     */
    public void addState(FSMAction entryAction, int stateId, JSONObject data,
                         FSMAction exitAction)
    {
        if(!states.containsKey(stateId))
        {
            State s = new State(stateId, data);
            s.setStateEntryAction(entryAction);
            s.setStateExitAction(exitAction);
            states.put(stateId, s);
        }
        else throw new RuntimeException("state; " + stateId +
                " already declared");
    }

    public void addState(FSMAction entryAction, int stateId, JSONObject data,
                         FSMAction exitAction, ExpectedInputType expectedInputType)
    {
        if(!states.containsKey(stateId))
        {
            State s = new State(stateId, data, expectedInputType);
            s.setStateEntryAction(entryAction);
            s.setStateExitAction(exitAction);
            states.put(stateId, s);
        }
        else throw new RuntimeException("state; " + stateId +
                " already declared");
    }

    /**
     * This method can be used to add an event to the FSM.
     * FSM uses the Event class to create a new Event object
     * @param name This is the name of the event.
     */
    public void addEvent(String name, List<AudioFileWithSpeechRates> audioData, FSMAction defaultAction)
    {
        if(!events.containsKey(name))
        {
            if(defaultAction != null)
                events.put(name, new Event(name, audioData, defaultAction));
            else
                events.put(name, new Event(name, audioData));
        }
//        else throw new RuntimeException("event; " + name +
//                " already declared");
    }
    public void addOpsEvent(String name, List<AudioFileWithSpeechRates> audioData, FSMAction action){
        if(!opsEvents.containsKey(name)){
            if(action != null)
                opsEvents.put(name, new Event(name, audioData, action));
            else
                opsEvents.put(name, new Event(name, audioData));
        }
        else throw new RuntimeException("opsEvent: " + name +
                " already declared");
//        addEvent(name, audioData, action);
    }

    /**
     * This method creates a transition between the sourcestate and the
     * target state. The method checks whether the given states and the
     * event exist before it creates the transition. If they don't exist
     * a RuntimeException is thrown.
     * @param sourcestate The ID of the sourcestate
     * @param eventname The name of the event that triggers the transition
     * @param targetstate the ID of the targetstate
     * @param action The action that will be executed when the transition
     * is triggered.
     */
    public void addTransition(int sourcestate, String eventname,
                              int targetstate, FSMAction action)
    {
        if(states.containsKey(sourcestate))
        {
            State s = (State)states.get(sourcestate);
            if(states.containsKey(targetstate))
            {
                State t = (State)states.get(targetstate);
                if(events.containsKey(eventname))
                {
                    s.addTransition(eventname, t, action);
                }
                else throw new RuntimeException("event; " +
                        eventname + " not found");
            }
            else
                throw new RuntimeException("state; " + targetstate
                    + " not found");
        }
        else throw new RuntimeException("state; " + sourcestate +
                " not found");
    }

    public void addFSMTransition(int sourceState, String eventName, String targetFSMId, String targetFSMType, FSMAction action){
        if(states.containsKey(sourceState)){
            State s = (State) states.get(sourceState);
            if(events.containsKey(eventName)) {
                s.addFSMTransition(eventName, targetFSMId, targetFSMType, action);
            }
            else throw new RuntimeException("event; " +
                    eventName + " not found");
        }
        else throw new RuntimeException("state; " + sourceState +
                " not found");
    }

    /**
     * This method is used to set the default state for the FSM. If a
     * FSMContext is created, this state is set as the current state.
     * @param stateId The ID of the first state.
     */
    public void setFirstState(int stateId)
    {
        first = (State)states.get(stateId);
    }

    public State getFirstState(){
        return this.first;
    }

    /**
     * Sometimes it's necessary to do some initialization before the FSM can
     * be used. For this purpose a initial action can be set. This action is
     * executed when the FSMContext is created.
     * @param action The initial action.
     */
    public void setInitAction(FSMAction action)
    {
        initaction = action;
    }

    /**
     * This method serves as a factory method to create FSMContexts from
     * the FSM. Also the init action is run (if available).
     * @return A new FSMContext for the FSM.
     */
    public FSMContext createFSMContext(String id)
    {
        FSMContext fsmc;
        if(first == null) throw new Error("first state not set");
        else
        {
            if(initaction != null)
            {
                fsmc =  new FSMContext(id, first, fsmContextController, initaction);
            }
            else
            {
                fsmc =  new FSMContext(id, first, fsmContextController);
            }
            return fsmc;
        }
    }

    public State getStateWithId(int id){
        return (State)states.get(id);
    }
    public List<Event> getEvents(){
        List<Event> res = new ArrayList<>();
        Enumeration eventsEnum = this.events.keys();
        while(eventsEnum.hasMoreElements()){
            String key = String.valueOf(eventsEnum.nextElement());
            Event event = (Event) this.events.get(key);
            res.add(event);
        }
        return res;
    }


    public List<Event> getOpsEvents(){
        List<Event> res = new ArrayList<>();
        Enumeration eventsEnum = this.opsEvents.keys();
        while(eventsEnum.hasMoreElements()){
            String key = String.valueOf(eventsEnum.nextElement());
            Event event = (Event) this.opsEvents.get(key);
            res.add(event);
        }
        return res;
    }

    public List<Event> getStateEvents(List<String> currStateEvents){
        return currStateEvents.stream().
                map(eventName -> (Event) this.events.get(eventName))
                .collect(Collectors.toList());
    }
}

