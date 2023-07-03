package com.microsoft.seeds.place.models.fsm;

import org.json.JSONObject;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class AutoStateDispatchThread {
    private static final Logger loggger = Logger.getLogger(AutoStateDispatchThread.class.getName());
    private static AutoStateDispatchThread instance;
    private Hashtable<String, String> fsmContextExecutionStatus;
    private Hashtable<String, String> fsmIdToEvent;

    private Hashtable<String, FSMContext> fsmContexts;
    private Hashtable<String, Long> fsmIdToLastExecutionTime;
    private Clock clock;

    private Thread thread;
    private AtomicBoolean busy;
    private AutoStateDispatchThread(){
        busy = new AtomicBoolean(false);
        fsmContextExecutionStatus = new Hashtable<>();
        fsmContexts = new Hashtable<>();
        fsmIdToEvent = new Hashtable<>();
        fsmIdToLastExecutionTime = new Hashtable<>();
        clock = Clock.systemDefaultZone();
        Runnable runnable = () -> run();
        thread = new Thread(runnable);
    }

    public static AutoStateDispatchThread getInstance(){
        if(instance == null){
            instance = new AutoStateDispatchThread();
        }
        return instance;
    }

    public void registerFSMContext(FSMContext baseFsmContext, String eventToDispatch){
        setBusy(false, true);

        String fsmId = baseFsmContext.getId();
        fsmContexts.put(fsmId, baseFsmContext);
        fsmIdToEvent.put(fsmId, eventToDispatch);
        fsmContextExecutionStatus.put(fsmId, FSMRunningStatus.RUNNING);
        fsmIdToLastExecutionTime.put(fsmId, clock.millis());

        setBusy(true, false);
        if(!thread.isAlive()){
            Runnable runnable = () -> run();
            thread = new Thread(runnable);
            thread.start();
        }
    }

    public void run() {
        while (true) {
            if(!busy.get()) {
                if (fsmContexts.isEmpty()) {
                    break;
                }
                Enumeration<String> keysEnum = fsmContexts.keys();
                while (keysEnum.hasMoreElements()) {
                    String fsmId = keysEnum.nextElement();
                    FSMContext fsmc = fsmContexts.get(fsmId);
                    JSONObject stateData = fsmc.getCurrState().getData();
                    if (stateData != null && stateData.has("time")) {
                        long currStateTime = stateData.getLong("time");
                        if(clock == null){
                            loggger.severe("CLOCK IS NULL");
                        }
                        if(fsmIdToLastExecutionTime == null){
                            loggger.severe("fsmIdToLastExecutionTime is NULL");
                        }
                        if (fsmContextExecutionStatus == null) {
                            loggger.severe("fsmContextExecutionStatus is NULL");
                        }
                        if (fsmContextExecutionStatus.get(fsmId).equals(FSMRunningStatus.RUNNING)
                                && (clock.millis() - fsmIdToLastExecutionTime.get(fsmId)) >= currStateTime * 1000) {
                            loggger.info("DISPATCHING " + fsmIdToEvent.get(fsmId) + " EVENT FOR FSM CONTEXT ID: " + fsmId + " AT " + clock.millis());
                            fsmc.dispatch(fsmIdToEvent.get(fsmId), null);
                            fsmIdToLastExecutionTime.put(fsmId, clock.millis());
                        }
                    }
                }
            }
        }
    }

    public void pauseExecution(String fsmId){
        setBusy(false, true);

        fsmContextExecutionStatus.put(fsmId, FSMRunningStatus.PAUSED);

        setBusy(true, false);
    }

    public void resumeExecuting(String fsmId){
        setBusy(false, true);

        fsmContextExecutionStatus.put(fsmId, FSMRunningStatus.RUNNING);
        fsmIdToLastExecutionTime.put(fsmId, clock.millis());

        setBusy(true, false);
    }

    public void stopExecution(String fsmId){
        setBusy(false, true);

        fsmContexts.remove(fsmId);
        fsmIdToEvent.remove(fsmId);
        fsmContextExecutionStatus.remove(fsmId);
        fsmIdToLastExecutionTime.remove(fsmId);

        setBusy(true, false);
    }

    public void stopAllExecution(){
        setBusy(false, true);

        fsmContexts.clear();
        fsmIdToEvent.clear();
        fsmContextExecutionStatus.clear();
        fsmIdToLastExecutionTime.clear();

        setBusy(true, false);
    }

    private void setBusy(boolean expect, boolean update){
        while(!busy.compareAndSet(expect, update));
    }
}