package com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller;

import com.microsoft.seeds.fsmexecutor.models.fsm.Event;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.fsm.State;

import java.util.List;

public interface FSMContextController {
    /**
     * Dispatch an event e.
     * @param eventName The event
     * @param data Some additional data
     * @param fsmc FSM Context
     */
    void dispatch(String eventName, Object data, FSMContext fsmc);
    List<Event> getOpsEvents();
    String getType();
    List<Event> getClientStateEvents(FSMContext fsmc, List<String> currStateEvents);

    String getLanguage();

    void updateExpFSMStackAndCurrStateMap(FSMContext fsmc, ExpFSM fsm, State lastState);

    boolean canPopFSM();

    void initialize(FSMContext fsmc);
}
