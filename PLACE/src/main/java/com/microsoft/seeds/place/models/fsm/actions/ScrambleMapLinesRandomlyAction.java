package com.microsoft.seeds.place.models.fsm.actions;

import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMContext;
import com.microsoft.seeds.place.models.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ScrambleMapLinesRandomlyAction implements FSMAction {

    private Logger logger = Logger.getLogger(ScrambleMapLinesRandomlyAction.class.getName());
    private int startStateId;
    private int len;

    public ScrambleMapLinesRandomlyAction() {
    }

    public ScrambleMapLinesRandomlyAction(int startLinesStateId, int len){
        this.startStateId = startLinesStateId;
        this.len = len;
    }

    @Override
    public void execute(FSMContext fsmc, Object data) {
        List<Integer> oneToNArray = getIndexArr(1, len);
        Collections.shuffle(oneToNArray);
        fsmc.put(Constants.SCRAMBLE_CORRECT_ORDER_LINES_KEY, oneToNArray);
        logger.info("CORRECT ORDER: " + oneToNArray);
        AtomicInteger index = new AtomicInteger(1);
        oneToNArray.forEach(shuffledNum ->
                fsmc.put(Constants.SCRAMBLE_KEY_MAPPINGS_KEY_PREFIX + index.getAndIncrement(), shuffledNum));
    }

    private List<Integer> getIndexArr(int start, int len){
        int temp = start;
        List<Integer> res = new ArrayList<>();
        for(int i = 0; i<len; ++i){
            res.add(temp++);
        }
        return res;
    }

    @Override
    public List<String> getActionName() {
        return new ArrayList<>(Collections.singletonList("ScrambleMapLinesRandomlyAction"));
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        return new ScrambleMapLinesRandomlyAction(args.getInt("startStateId"), args.getInt("len"));
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        JSONObject args = new JSONObject();
        args.put("startStateId", startStateId);
        args.put("len", len);
        json.put(JSON_NAME_KEY, getActionName().get(0));
        json.put(JSON_ARGS_KEY, args);
        array.put(json);
        return array;
    }
}
