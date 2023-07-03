package com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller;

import com.microsoft.seeds.fsmexecutor.models.fsm.Event;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import com.microsoft.seeds.fsmexecutor.models.fsm.State;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.ScrambleStateTypes;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScrambleFSMContextController implements FSMContextController{
    private static final Logger logger = Logger.getLogger(ScrambleFSMContextController.class.getName());
    private ExpFSM theFSM;
    public ScrambleFSMContextController(ExpFSM theFSM) {
        this.theFSM = theFSM;
    }
    @Override
    public void dispatch(String eventName, Object data, FSMContext fsmc) {
        JSONObject currStateData = fsmc.getCurrState().getData();
        if(currStateData != null && currStateData.has(ScrambleStateTypes.TYPE_KEY)){
            switch (currStateData.getString(ScrambleStateTypes.TYPE_KEY)){
                case ScrambleStateTypes.ANSWER_CHECK:
                    try {
                        List<Integer> inputOrder = Arrays
                                .stream(eventName.split("\\B"))
                                .map(Integer::valueOf)
                                .collect(Collectors.toList());
                        logger.info("GOT USER ANSWER: " + inputOrder.toString());
                        if(fsmc.containsKey(Constants.SCRAMBLE_CORRECT_ORDER_LINES_KEY)){
                            logger.info("CORRECT ANSWER IS: " + fsmc.get(Constants.SCRAMBLE_CORRECT_ORDER_LINES_KEY));
                            if(inputOrder.equals(fsmc.get(Constants.SCRAMBLE_CORRECT_ORDER_LINES_KEY))){
                                dispatchScrambleEvent(ScrambleStateTypes.CORRECT_EVENT, data, fsmc);
                            }else{
                                dispatchScrambleEvent(ScrambleStateTypes.INCORRECT_EVENT, data, fsmc);
                            }
                        }
                    }catch (Exception e) {
                        dispatchScrambleEvent(eventName, data, fsmc);
                    }
                    break;
                case ScrambleStateTypes.GET_LINES:
                    try{
                        String key = getKey(Integer.parseInt(eventName));
                        if(fsmc.containsKey(key)){
                            dispatchScrambleEvent(String.valueOf(fsmc.get(key)), data, fsmc);
                        }
                    }
                    catch (Exception ex) {
                        dispatchScrambleEvent(eventName, data, fsmc);
                    }
                    break;
                default:
                    dispatchScrambleEvent(eventName, data, fsmc);
            }
        }
    }

    private String getKey(int index){
        return Constants.SCRAMBLE_KEY_MAPPINGS_KEY_PREFIX + index;
    }

    private void dispatchScrambleEvent(String eventName, Object data, FSMContext fsmc){
        if(fsmc.getCurrState().isEndState()){
            fsmc.executeFSMAbortAction(null);
            return;
        }
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
