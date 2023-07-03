package com.microsoft.seeds.fsmexecutor.models.fsm.generators;

import com.microsoft.seeds.fsmexecutor.models.fsm.*;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.*;
import com.microsoft.seeds.fsmexecutor.models.request.CreateScrambleRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScrambleGenerator extends FSMGeneratorAPI {
    private static final String ENTER_SEQ_EVENT_NAME = "Enter Seq";
    private static final String REPLAY_POEM_EVENT_NAME = "Replay Poem";
    private static final long ENTER_SEQ_WAIT_TIME = 10000;

    public static ExpFSM getFSM(CreateScrambleRequest createScrambleRequest){
        if(!createScrambleRequest.isValid()){
            return null;
        }
        ExpFSM fsm = new ExpFSM(createScrambleRequest.getId(), 1,
                Constants.SCRAMBLE_FSM_TYPE, createScrambleRequest.getLanguage());

        FSMActionList entryStateAction = new FSMActionList()
                .add(new StdOutAction())
                .add(new PushFSMStateAction());
        FSMAction entryEndStateAction = new FSMActionList()
                .add(new PushFSMStateAction())
                .add(new PushFSMContextDataAction())
//                .add(new StopAction())
                .add(new AutoPopFSMAction());

        // ADDING REPEAT POEM EVENT
        createEvent(getReplayPoemEvent(createScrambleRequest.language), fsm);

        // ADDING ENTER SEQUENCE EVENT
        createEvent(getEnterSeqEvent(createScrambleRequest.language), fsm);

        // ADDING AUTO EVENT
        createEvent(new Event(Constants.AUTO_EVENT, null), fsm);

        // ADDING TIMEOUT EVENT
        createEvent(new Event(Constants.TIMEOUT_EVENT, null), fsm);

        // ADDING ABORT OPS EVENT
        createOpsEvent(new Event(Constants.ABORT_EVENT, null) ,fsm);

        // ADDING CORRECT ANSWER EVENT
        createEvent(new Event(ScrambleStateTypes.CORRECT_EVENT, null), fsm);

        // ADDING INCORRECT ANSWER EVENT
        createEvent(new Event(ScrambleStateTypes.INCORRECT_EVENT, null), fsm);

        // ADDING EVENTS THAT GETS USER THE MAPPED LINES
        for(int k = 1; k <= createScrambleRequest.lines.size(); ++k){
            createEvent(getKeyPressEvent(k), fsm);
        }

        // FIRST STATE WITH FULL AUDIO AND PRESS KEYS BETWEEN PROMPT
        createState(entryStateAction.add(new AutoEventDispatchAction()), 1,
                getStateData(ScrambleStateTypes.AUDIO,
                        createScrambleRequest.getFullAudioFileDescription(getPressAnyKeysBetween(createScrambleRequest.language, createScrambleRequest.lines.size()))),
                new SkipAction(), ExpectedInputType.NIL, fsm);
        fsm.setFirstState(1);

        // GET LINES STATE THAT SENDS STATES WITH POEM LINES
        int getLinesStateId = 2;
        createState(entryStateAction, getLinesStateId, getStateData(ScrambleStateTypes.GET_LINES, new ArrayList<>()), new SkipAction(), ExpectedInputType.EVENT_NAME, fsm);

        // AUTOMATICALLY GO TO GET LINES STATE FROM FULL AUDIO STATE
        createTransition(1, getLinesStateId, Constants.AUTO_EVENT, new SkipAction(), fsm);

        //STATES WITH POEM LINES AND SET INIT ACTION
        int i = 3;
        fsm.setInitAction(new ScrambleMapLinesRandomlyAction(i, createScrambleRequest.getLines().size()));
        List<AudioFileWithSpeechRates> linesAudioFileDescriptions = createScrambleRequest.getLinesAudioFileDescriptions();
        for (int lineNum = 0; lineNum<linesAudioFileDescriptions.size(); ++lineNum) {
            int newStateId = i++;
            createState(
                    entryStateAction.add(new AutoEventDispatchAction()),  // DISPATCH AUTO EVENT AFTER PUSHING THE LINE
                    newStateId,
                    getStateData(ScrambleStateTypes.AUDIO, Collections.singletonList(linesAudioFileDescriptions.get(lineNum))),
                    new SkipAction(), ExpectedInputType.NIL, fsm);
            // GOING FROM GET LINES STATE TO ABOVE POEM LINE STATE
            createTransition(getLinesStateId, newStateId, String.valueOf(lineNum + 1), new SkipAction(), fsm);
            // GOING FROM ABOVE POEM LINE STATE TO GET LINES STATE
            createTransition(newStateId, getLinesStateId, Constants.AUTO_EVENT, new SkipAction(), fsm);
        }

        // ANSWER CHECK STATE -> First pushes the GuessTheSequence prompt and expects a string of numbers pressed
        int checkAnswerStateId = i++;
        FSMAction enterSeqTimeoutAction = new ExecuteAfterWaitTimeAction(
                ENTER_SEQ_WAIT_TIME,
                checkAnswerStateId,
                new TimeoutEventDispatchAction());
        createState(entryStateAction.add(enterSeqTimeoutAction), checkAnswerStateId,
                getStateData(ScrambleStateTypes.ANSWER_CHECK, CreateScrambleRequest.getAudioFileDescriptionWithSpeechRates(
                        Collections.singletonList(getGuessTheSequence(createScrambleRequest.language)))),
                new SkipAction(), createScrambleRequest.getExpectedInputTypeForAnswerCheckState(), fsm);

        // GOING FROM GET LINES STATE TO CHECK ANSWER STATE
        createTransition(getLinesStateId, checkAnswerStateId, ENTER_SEQ_EVENT_NAME, new SkipAction(), fsm);

        // GUESS SEQ TIMEOUT STATE
        int guessSeqTimeOutStateId = i++;
        createState(
                entryStateAction.add(new AutoEventDispatchAction()),
                guessSeqTimeOutStateId,
                getStateData(ScrambleStateTypes.AUDIO, CreateScrambleRequest.getAudioFileDescriptionWithSpeechRates(
                        Collections.singletonList(getYouDidntGuessAnySeq(createScrambleRequest.language)))),
                new SkipAction(), ExpectedInputType.NIL, fsm);

        // GOING FROM GUESS SEQ TIMEOUT STATE TO CHECK ANSWER STATE ON TIMEOUT
        createTransition(guessSeqTimeOutStateId, checkAnswerStateId, Constants.AUTO_EVENT, new SkipAction(), fsm);

        // GOING FROM CHECK ANSWER STATE TO GUESS SEQ TIMEOUT STATE ON TIMEOUT
        createTransition(checkAnswerStateId, guessSeqTimeOutStateId, Constants.TIMEOUT_EVENT, new SkipAction(), fsm);

        // GOING FROM GUESS SEQ TIMEOUT STATE TO ANSWER CHECK STATE
        createTransition(guessSeqTimeOutStateId, checkAnswerStateId, ENTER_SEQ_EVENT_NAME, new SkipAction(), fsm);

        // GOING FROM GUESS SEQ TIMEOUT STATE TO FULL AUDIO STATE
        createTransition(guessSeqTimeOutStateId, 1, REPLAY_POEM_EVENT_NAME, new SkipAction(), fsm);

        // CORRECT ANSWER STATE
        int correctAnswerStateId = i++;
        createState(
                entryStateAction.add(new AutoEventDispatchAction()),
                correctAnswerStateId,
                getStateData(ScrambleStateTypes.AUDIO,
                        CreateScrambleRequest.getAudioFileDescriptionWithSpeechRates(
                                new ArrayList<>(Arrays.asList(getCorrectAnswer(createScrambleRequest.language), createScrambleRequest.fullAudio)))),
                new SkipAction(), ExpectedInputType.NIL, fsm);

        // GOING FROM ANSWER CHECK STATE TO CORRECT ANSWER STATE
        createTransition(checkAnswerStateId, correctAnswerStateId, ScrambleStateTypes.CORRECT_EVENT, new SkipAction(), fsm);

        // WRONG ANSWER STATE
        int incorrectAnswerStateId = i++;
        FSMAction incorrectAnswerTimeoutAction = new ExecuteAfterWaitTimeAction(
                ENTER_SEQ_WAIT_TIME,
                incorrectAnswerStateId,
                new TimeoutEventDispatchAction());
        createState(
                entryStateAction.add(incorrectAnswerTimeoutAction),
                incorrectAnswerStateId,
                getStateData(ScrambleStateTypes.AUDIO,
                        CreateScrambleRequest.getAudioFileDescriptionWithSpeechRates(Collections.singletonList(getWrongAnswer(createScrambleRequest.language)))),
                new SkipAction(), ExpectedInputType.EVENT_NAME, fsm);

        // GOING FROM INCORRECT ANSWER STATE TO SAME STATE ON TIMEOUT
        createTransition(incorrectAnswerStateId, incorrectAnswerStateId, Constants.TIMEOUT_EVENT, new SkipAction(), fsm);

        // GOING FROM ANSWER CHECK STATE TO INCORRECT ANSWER STATE
        createTransition(checkAnswerStateId, incorrectAnswerStateId, ScrambleStateTypes.INCORRECT_EVENT, new SkipAction(), fsm);

        // GOING FROM INCORRECT ANSWER STATE TO ANSWER CHECK STATE
        createTransition(incorrectAnswerStateId, checkAnswerStateId, ENTER_SEQ_EVENT_NAME, new SkipAction(), fsm);

        // GOING FORM INCORRECT ANSWER STATE TO FULL AUDIO STATE
        createTransition(incorrectAnswerStateId, 1, REPLAY_POEM_EVENT_NAME, new SkipAction(), fsm);

        // END STATE
        createState(entryEndStateAction, i, null, new SkipAction(), ExpectedInputType.NIL, fsm); //END STATE

        // GOING FROM CORRECT ANSWER STATE TO END STATE
        createTransition(correctAnswerStateId, i, Constants.AUTO_EVENT, new SkipAction(), fsm);

        return fsm;
    }

    private static JSONObject getStateData(String stateType, List<AudioFileWithSpeechRates> audioFiles){
        JSONObject res = new JSONObject();
        res.put(ScrambleStateTypes.TYPE_KEY, stateType);
        JSONArray audioArray = new JSONArray();
        for(AudioFileWithSpeechRates audioFileWithSpeechRates : audioFiles){
            audioArray.put(audioFileWithSpeechRates.toJSON());
        }
        res.put(Constants.AUDIO_DATA_KEY, audioArray);
        return res;
    }

    private static String getBasePath(String language){
        return "pull-model-menus/experienceSpecificDialog/" + language.toLowerCase() + "/scramble/";
    }

    private static String getGuessTheSequence(String language){
        return getBasePath(language) + "guessSequence/enter the sequence which you guess as a correct order";
    }

    private static String getCorrectAnswer(String language){
        return getBasePath(language) + "messageForCorrectSequence/Congrats!, you have guessed the Sequence Correctly. We are playing the entire Poem again just for a refresh";
    }

    private static String getPressAnyKeysBetween(String language, int numLines){
        return getBasePath(language) + "pressAnyKeybetween1and"+ numLines +"/Press any key between 1 and "+ numLines +" to listen to the corresponding mapped lines of this played poem";
    }

    private static String getYouDidntGuessAnySeq(String language){
        return getBasePath(language) + "youDidNotGuessAnySeq/You Didn't guess any sequence,";
    }

    private static String getWrongAnswer(String language){
        return "pull-model-menus/chosenWrongOptionDialog/"+ language+ "/Sorry, you have chosen the wrong option";
    }

    private static Event getReplayPoemEvent(String language){
        String fp = getBasePath(language) + "replayContent/to replay the Rhyme";
        return new Event(REPLAY_POEM_EVENT_NAME, Collections.singletonList(AudioFileWithSpeechRates.getFor(fp)));
    }

    private static Event getEnterSeqEvent(String language){
        String fp = getBasePath(language) + "enterSequence/to enter the Sequence";
        return new Event(ENTER_SEQ_EVENT_NAME, Collections.singletonList(AudioFileWithSpeechRates.getFor(fp)));
    }

    private static Event getKeyPressEvent(int num){
        return new Event(String.valueOf(num), null);
    }

}
