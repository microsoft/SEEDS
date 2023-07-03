package com.microsoft.seeds.fsmexecutor.models.fsm.generators;

import com.microsoft.seeds.fsmexecutor.models.fsm.Event;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.*;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.PullModelFSMContextController;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelLeafNodeOption;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelNode;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelNodeOption;
import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import com.microsoft.seeds.fsmexecutor.models.utils.PullModelMenuAudioPaths;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PullModelFSMGenerator extends FSMGeneratorAPI {
    private static final String GO_BACK = "GO_BACK";
    private static final String SELECT = "SELECT";
    private static final String NEXT = "NEXT";
    private static final String AUTO = Constants.AUTO_EVENT;
    private static final String GO_TO_PREV_MENU = "GO_TO_PREV_MENU";

    private static final String REPEAT_MENU = "REPEAT_MENU";
    private static final FSMActionList entryStateAction = new FSMActionList()
            .add(new StdOutAction())
            .add(new PushFSMStateAction());
    public static ExpFSM getFSM(PullModelData pullModelData, String language){
        ExpFSM fsm = new ExpFSM(pullModelData.getId(), 1, Constants.PULL_MODEL_FSM_TYPE, language);
        fsm.setInitAction(new SkipAction());

        createEvent(new Event(SELECT, Collections.singletonList(AudioFileWithSpeechRates.getFor(PullModelMenuAudioPaths.SELECT_CURR_TITLE))), fsm);
        createEvent(new Event(GO_BACK,  Collections.singletonList(AudioFileWithSpeechRates.getFor(PullModelMenuAudioPaths.GO_BACK_PREV_TITLE))), fsm);
        createEvent(new Event(NEXT,  Collections.singletonList(AudioFileWithSpeechRates.getFor(PullModelMenuAudioPaths.NEXT_TITLE))), fsm);
        createEvent(new Event(REPEAT_MENU, Collections.singletonList(AudioFileWithSpeechRates.getFor(PullModelMenuAudioPaths.REPEAT_CURR_MENU))), fsm);
        createEvent(new Event(GO_TO_PREV_MENU, Collections.singletonList(AudioFileWithSpeechRates.getFor(PullModelMenuAudioPaths.GO_TO_PREV_MENU))), fsm);
        createEvent(new Event(AUTO, null), fsm);

        createOpsEvent(new Event(PullModelFSMContextController.POP_FSM_EVENT, null), fsm);

        Stack<Integer> stateIdsStack = getStateIds(pullModelData.getNumOptionsFromNode(pullModelData.getRootNode()) + 2 ); // + 2 for welcome and root options state
        int welcomeStateId = stateIdsStack.pop();
        // WELCOME TO SEEDS STATE
        createState(entryStateAction
                        .add(new AutoEventDispatchAction()),
                welcomeStateId,
                getStateData(pullModelData.getInitialMessage()),
                new SkipAction(),
                ExpectedInputType.NIL, fsm);

        recur(pullModelData.getRootNode(), welcomeStateId, pullModelData, fsm, stateIdsStack);
        createTransition(1, 2, AUTO, new SkipAction(), fsm); // AUTO FROM WELCOME SEEDS TO MENU
        fsm.setFirstState(1);
        return fsm;
    }

    private static void recur(PullModelNode currNode, int parentStateId, PullModelData pullModelData, ExpFSM fsm, Stack<Integer> stateIds){
        // CREATE NEW STATE
        List<Integer> newStateIdsForLeafOptions = new ArrayList<>();
        int rootStateId = stateIds.pop();
        createState(entryStateAction,
                rootStateId,
                getStateData(currNode.getAudioFileWithSpeechRatesList()),
                new SkipAction(),
                ExpectedInputType.EVENT_NAME, fsm);

        int prevStateId = rootStateId;
        boolean hasLeafOptions = false;
        for (PullModelNodeOption option : currNode.getOptions()) {
            if (!PullModelNodeOption.isLeafNodeOption(option)) {
                // CREATE NEW EVENT
                createEvent(new Event(option.getId(),
                                Collections.singletonList(option.getAudioData())),
                        fsm);

                // RECUR TO CREATE NEW CHILD STATE
                String childNodeKey = PullModelData.getKey(option.getId(), currNode.getId());
                PullModelNode childNode = pullModelData.getNode(childNodeKey);
                int childStateId = stateIds.peek();
                recur(childNode, rootStateId, pullModelData, fsm, stateIds);

                // CREATE TRANSITION ON THE CURR NODE TO CHILD NODE
                createTransition(rootStateId, childStateId, option.getId(), new SkipAction(), fsm);
            } else {
                hasLeafOptions = true;
                int newStateId = stateIds.pop();
                newStateIdsForLeafOptions.add(newStateId);

                // CREATE NEW STATE
                createState(entryStateAction,
                        newStateId,
                        getStateData(Collections.singletonList(option.getAudioData())),
                        new SkipAction(),
                        ExpectedInputType.NIL,
                        fsm);

                // CREATE TRANSITION ON GO_BACK
                if(prevStateId != rootStateId) {
                    // GO TO PREV TITLE
                    createTransition(newStateId, prevStateId, GO_BACK, new SkipAction(), fsm);
                }else{
                    // BE ON THE SAME TITLE ON GO_BACK EVENT ON FIRST TITLE
                    createTransition(newStateId, newStateId, GO_BACK, new SkipAction(), fsm);
                    createTransition(rootStateId, rootStateId, GO_BACK, new SkipAction(), fsm);
                    createTransition(rootStateId, rootStateId, SELECT, new SkipAction(), fsm);
                }

                // FSM TRANSITION ON THE CURR NODE TO CHILD FSM
                createFSMTransition(SELECT,
                        fsm, newStateId,
                        option.getId(),
                        ((PullModelLeafNodeOption) option).getFsmType(),
                        new SkipAction());

                // CREATE TRANSITION ON NEXT
                createTransition(prevStateId, newStateId, NEXT, new SkipAction(), fsm);
                createTransition(newStateId, newStateId, NEXT, new SkipAction(), fsm); // OVERWRITTEN IN NEXT ITERATION

                prevStateId = newStateId;
            }
        }

        if(hasLeafOptions) {
            int index = 0;
            for (PullModelNodeOption option : currNode.getOptions()) {
                if (PullModelNodeOption.isLeafNodeOption(option)) {
                    int newStateId = newStateIdsForLeafOptions.get(index++);
                    // CREATE TRANSITION ON GO_TO_PREV_MENU
                    createTransition(newStateId, parentStateId, GO_TO_PREV_MENU, new SkipAction(), fsm);
                }
            }
        }else{
            createTransition(rootStateId, rootStateId, REPEAT_MENU, new SkipAction(), fsm);
        }

        // THIS IS NOT ROOT OPTIONS STATE
        if(parentStateId > 1) {
            // EXIT TO PARENT STATE
            createTransition(rootStateId, parentStateId, GO_TO_PREV_MENU, new SkipAction(), fsm);
        }

    }

    private static Stack<Integer> getStateIds(int statesNum){
        List<Integer> stateIdRange = IntStream.rangeClosed(1, statesNum)   // Starting from 2 as 1 is the welcome message state
                .boxed().collect(Collectors.toList());
        Collections.reverse(stateIdRange);
        Stack<Integer> stateIds = new Stack<>();
        stateIdRange.forEach(id -> stateIds.push(id));
        return stateIds;
    }

    private static JSONObject getStateData(List<AudioFileWithSpeechRates> audioFiles){
        JSONObject res = new JSONObject();
        JSONArray audioArray = new JSONArray();
        for(AudioFileWithSpeechRates audioFileWithSpeechRates : audioFiles){
            audioArray.put(audioFileWithSpeechRates.toJSON());
        }
        res.put(Constants.AUDIO_DATA_KEY, audioArray);
        return res;
    }
}
