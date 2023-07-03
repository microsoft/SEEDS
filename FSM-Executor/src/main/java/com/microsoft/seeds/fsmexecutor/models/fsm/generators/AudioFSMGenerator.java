package com.microsoft.seeds.fsmexecutor.models.fsm.generators;

import com.microsoft.seeds.fsmexecutor.models.fsm.Event;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.*;
import com.microsoft.seeds.fsmexecutor.models.request.CreateAudioFSMRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import org.json.JSONObject;

import java.util.Collections;

// SONGS, STORIES, RHYMES, POEMS
public class AudioFSMGenerator extends FSMGeneratorAPI {
    public static ExpFSM getFSM(CreateAudioFSMRequest createAudioFSMRequest){
        String finishedAudioEventName= "finished_audio";

        if(!createAudioFSMRequest.isValid()){
            return null;
        }
        ExpFSM fsm = new ExpFSM(createAudioFSMRequest.getId(), 1, createAudioFSMRequest.getType(), createAudioFSMRequest.getLanguage());
        fsm.setInitAction(new SkipAction());
        FSMActionList entryStateAction = new FSMActionList()
                .add(new StdOutAction())
                .add(new PushFSMStateAction());
        FSMActionList entryEndStateAction = entryStateAction
                .add(new PushFSMContextDataAction())
                .add(new AutoPopFSMAction());
        FSMAction exitStateAction = new SkipAction();
        JSONObject data = getStateData(createAudioFSMRequest.getAudioFiles());

        createEvent(new Event(finishedAudioEventName, null), fsm);

        createState(entryStateAction, 1, data, exitStateAction, ExpectedInputType.NIL, fsm);
        createState(entryEndStateAction, 2, null, exitStateAction, ExpectedInputType.NIL, fsm);
        createTransition(1, 2, finishedAudioEventName, new SkipAction(), fsm);
        fsm.setFirstState(1);
        return fsm;
    }

    private static JSONObject getStateData(AudioFileWithSpeechRates audioFiles){
        JSONObject res = new JSONObject();
        res.put("audioData", Collections.singletonList(audioFiles.toJSON()));
        return res;
    }
}
