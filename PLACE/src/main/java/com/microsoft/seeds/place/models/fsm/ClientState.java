package com.microsoft.seeds.place.models.fsm;

import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.ExpectedInputType;
import org.json.JSONObject;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientState {
    public int id;
    public String type;
    public Map<String, Object> data;
    public List<Map<String, Object>> events;
    public List<Map<String, Object>> opsEvents;
    public String abortEvent;
    public long time;
    public String fsmContextId;
    public boolean isEndState;
    public String userInputEp;

    public ExpectedInputType expectedInputType;
    private static Clock clock = Clock.systemDefaultZone();

    public ClientState() {
    }

    public ClientState(int id, Map<String, Object> data, boolean isEndState, ExpectedInputType expectedInputType) {
        this.id = id;
        this.data = data;
        this.expectedInputType = expectedInputType;
        this.events = new ArrayList<>();
        this.isEndState = isEndState;
        this.time = clock.millis();
        this.fsmContextId = "default";
        this.type = "default";
        this.userInputEp = "default";
        this.opsEvents = new ArrayList<>();
        this.abortEvent = Constants.ABORT_EVENT;
    }

    public ClientState(int id, Map<String, Object> data, boolean isEndState) {
        this.id = id;
        this.data = data;
        this.expectedInputType = ExpectedInputType.NIL;
        this.events = new ArrayList<>();
        this.isEndState = isEndState;
        this.time = clock.millis();
        this.fsmContextId = "default";
        this.type = "default";
        this.userInputEp = "default";
        this.opsEvents = new ArrayList<>();
        this.abortEvent = Constants.ABORT_EVENT;
    }

    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public ExpectedInputType getExpectedInputType() {
        return expectedInputType;
    }

    public void setExpectedInputType(ExpectedInputType expectedInputType) {
        this.expectedInputType = expectedInputType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public List<Map<String, Object>> getOpsEvents() {
        return opsEvents;
    }

    public String getAbortEvent() {
        return abortEvent;
    }

    public long getTime() {
        return time;
    }

    public String getFsmContextId() {
        return fsmContextId;
    }

    public boolean isEndState() {
        return isEndState;
    }

    public static Clock getClock() {
        return clock;
    }

    public void setEventsFromList(List<Event> events){
        this.events = new ArrayList<>();
//        System.out.println("SETTING CLIENT STATE EVENTS");
        events.forEach(event -> {
//            System.out.println("     " + event.name);
            this.events.add(event.getClientStateEventObj());
        });
    }

    public void setEvents(List<Map<String, Object>> events) {
        this.events = events;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFsmContextId(String fsmContextId) {
        this.fsmContextId = fsmContextId;
    }

    public String getUserInputEp() {
        return userInputEp;
    }

    public void setOpsEventsFromList(List<Event> opsEvents) {
        this.opsEvents = new ArrayList<>();
        opsEvents.forEach(event -> this.opsEvents.add(event.getClientStateEventObj()));
    }

    public void setOpsEvents(List<Map<String, Object>> opsEvents) {
        this.opsEvents = opsEvents;
    }


    public void setUserInputEp(String userInputEp) {
        this.userInputEp = userInputEp;
    }


    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("data", data);
        res.put("events", events);
        res.put("opsEvents", opsEvents);
        res.put("time", time);
        res.put("fsmContextId", fsmContextId);
        res.put("isEndState", isEndState);
        res.put("type", type);
        res.put("userInputEp", userInputEp);
        res.put("abortEvent", abortEvent);
        res.put("expectedInputType", expectedInputType.name());
        return res;
    }
}
