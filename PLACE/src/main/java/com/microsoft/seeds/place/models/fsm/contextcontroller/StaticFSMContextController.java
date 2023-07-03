package com.microsoft.seeds.place.models.fsm.contextcontroller;

import com.microsoft.seeds.place.models.fsm.Event;
import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import com.microsoft.seeds.place.models.fsm.State;
import com.microsoft.seeds.place.models.utils.Constants;

import java.util.List;
import java.util.logging.Logger;

public class StaticFSMContextController implements FSMContextController{
    private static final Logger logger = Logger.getLogger(StaticFSMContextController.class.getName());
    private ExpFSM theFSM;

    public StaticFSMContextController(ExpFSM theFSM) {
        this.theFSM = theFSM;
    }

    @Override
    public void dispatch(String eventName, Object data, FSMContext fsmc) {
        logger.info("DISPATCHING EVENT: " + eventName);
        if(theFSM == null){
            logger.info("FSM IS NULL!");
            return;
        }
        if(eventName.equals(Constants.ABORT_EVENT)){
            fsmc.executeFSMAbortAction(null);
            return;
        }
        if(theFSM.events.containsKey(eventName)) {
            Event event = (Event) theFSM.events.get(eventName);
            fsmc.getCurrState().dispatch(event, data, fsmc);
        }
        else throw new RuntimeException("event " +
                eventName + " not found in FSM");
    }

    @Override
    public List<Event> getOpsEvents() {
        return theFSM.getOpsEvents();
    }

    @Override
    public String getType(){
        return this.theFSM.getType();
    }

    @Override
    public List<Event> getClientStateEvents(FSMContext fsmc, List<String> currStateEvents) {
        return this.theFSM.getStateEvents(currStateEvents);
    }

    @Override
    public String getLanguage() {
        return this.theFSM.getLanguage();
    }

    @Override
    public void updateExpFSMStackAndCurrStateMap(FSMContext fsmc, ExpFSM fsm, State lastState) {

    }

    @Override
    public boolean canPopFSM() {
        return false;
    }

    @Override
    public void initialize(FSMContext fsmc) {

    }
}
