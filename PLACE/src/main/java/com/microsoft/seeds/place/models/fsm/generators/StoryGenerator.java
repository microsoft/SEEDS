package com.microsoft.seeds.place.models.fsm.generators;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMAction;
import com.microsoft.seeds.place.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.place.models.fsm.actions.*;
import com.microsoft.seeds.place.models.request.CreateStoryRequest;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.ExpectedInputType;
import com.microsoft.seeds.place.models.utils.StoryLine;
import org.json.JSONObject;

import java.util.List;

public class StoryGenerator extends FSMGeneratorAPI {

    public static ExpFSM getFSM(CreateStoryRequest storyRequest){
        ExpFSM story = new ExpFSM(storyRequest.getName(), 1, Constants.STORY_FSM_TYPE, "English");
        story.setInitAction(new SkipAction());
        List<StoryLine> lines = storyRequest.getStoryLines();
        FSMAction entryActionFirstState = new FSMActionList()
                .add(new RegisterFSMContextWithAutoDispatchThreadAction())
                .add(new StdOutAction())
                .add(new PushFSMStateAction());
        FSMAction entryActionNormalStates = new FSMActionList()
                .add(new StdOutAction())
                .add(new PushFSMStateAction());
        FSMAction entryActionEndState = new FSMActionList()
                .add(new PushFSMContextDataAction())
                .add(new PushFSMStateAction())
                .add(new StopAction())
                .add(new AutoPopFSMAction());
        int id = 1;
        for(StoryLine line: lines){
            createState(id == 1 ? entryActionFirstState : entryActionNormalStates, id,
                    getStoryStateData(line.getText(), line.getTime()), null, null, story);
            id++;
        }
        int endOfStateId = id;
        createState(entryActionEndState, endOfStateId, getSingleStringStateData("End of Story."), null, ExpectedInputType.NIL, story);
        story.setFirstState(1);

//        createEvent(Constants.AUTO_EVENT, new SkipAction(), story);
//        createEvent(Constants.STORY_PLAY_EVENT, new ResumeAction(), story);
//        createOpsEvent(Constants.STORY_PAUSE_EVENT, new PauseAction(), story);
//        createOpsEvent(Constants.EXIT_EVENT, FSMActionList.getExitActionList(), story);

        for(id = 1; id <= lines.size(); ++id){
            createTransition(id, id+1, Constants.AUTO_EVENT, new SkipAction(), story);
            createTransition(id, id, Constants.STORY_PLAY_EVENT, new ResumeAction(), story);
        }
        return story;
    }

    private static JSONObject getStoryStateData(String text, double time){
        JSONObject res = new JSONObject();
        res.put(Constants.STATE_DATA_TEXT_KEY, text);
        res.put("time", time);
        return res;
    }
}
