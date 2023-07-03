package com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller;

import com.microsoft.seeds.fsmexecutor.models.fsm.Event;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.fsm.State;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class PullModelFSMContextController implements FSMContextController {
    public static final String POP_FSM_EVENT = "POP_FSM";
    ExpFSM baseFSM;

    public PullModelFSMContextController(ExpFSM baseFSM) {
        this.baseFSM = baseFSM;
    }

    @Override
    public void dispatch(String eventName, Object data, FSMContext fsmc) {
        if(eventName.equals(POP_FSM_EVENT)){
            popFSMStackAndSetCurrState(fsmc);
            return;
        }
        if(eventName.equals(Constants.ABORT_EVENT)){
            abort(fsmc);
            return;
        }
        Stack<ExpFSM> currFSMStack = fsmc.getFsmStack();
        HashMap<String, State> currLastStateMap = fsmc.getFsmToLastStateMap();
        if(!currFSMStack.isEmpty()) {
            ExpFSM fsm = currFSMStack.peek();
            if (fsm.events.containsKey(eventName)) {
                Event event = (Event) fsm.events.get(eventName);
                fsmc.getCurrState().dispatch(event, data, fsmc);
                currLastStateMap.put(currFSMStack.peek().getId(), fsmc.getCurrState());
            }
            else throw new RuntimeException("event " +
                    eventName + " not found in FSM " + fsm.getId());
        }
        else throw new RuntimeException("FSM STACK IS EMPTY");
    }

    @Override
    public List<Event> getOpsEvents() {
        return this.baseFSM.getOpsEvents();
    }

    @Override
    public String getType() {
        return "PULL_MODEL";
    }

    @Override
    public List<Event> getClientStateEvents(FSMContext fsmc, List<String> currStateEvents) {
        Stack<ExpFSM> currFSMStack = fsmc.getFsmStack();
        if(currFSMStack.isEmpty()){
            throw new RuntimeException("FSM STACK IS EMPTY!!");
        }
        return currFSMStack.peek().getStateEvents(currStateEvents);
    }

    @Override
    public String getLanguage() {
        return this.baseFSM.getLanguage();
    }

    @Override
    public void updateExpFSMStackAndCurrStateMap(FSMContext fsmc, ExpFSM fsm, State lastState) {
        Stack<ExpFSM> currFSMStack = fsmc.getFsmStack();
        HashMap<String, State> currLastStateMap = fsmc.getFsmToLastStateMap();
        if(lastState != null) {
            currLastStateMap.put(currFSMStack.peek().getId(), lastState);
        }
        currFSMStack.push(fsm);
        currLastStateMap.put(fsm.getId(), fsm.getFirstState());
    }

    @Override
    public boolean canPopFSM() {
        return true;
    }

    @Override
    public void initialize(FSMContext fsmc) {
        // CLEARING UP EXISTING DATA
        fsmc.getFsmToLastStateMap().clear();
        fsmc.getFsmStack().clear();

        // ADDING BASE FSM TO STACK
        fsmc.getFsmStack().push(baseFSM);

        // SETTING LAST STATE TO FIRST STATE
        fsmc.getFsmToLastStateMap().put(baseFSM.getId(), baseFSM.getFirstState());
    }

    private void popFSMStackAndSetCurrState(FSMContext fsmc){
        Stack<ExpFSM> currFSMStack = fsmc.getFsmStack();
        HashMap<String, State> currLastStateMap = fsmc.getFsmToLastStateMap();
        currFSMStack.pop();
        if(!currFSMStack.isEmpty()){
            ExpFSM curr = currFSMStack.peek();
            fsmc.setCurrState(currLastStateMap.get(curr.getId()));
            fsmc.executeCurrentStateEntryAction(null);
        }else{
            abort(fsmc);
        }
    }

    private void abort(FSMContext fsmc){
        fsmc.getFsmToLastStateMap().clear();
        fsmc.getFsmStack().clear();
        fsmc.executeFSMAbortAction(null);
    }
}
