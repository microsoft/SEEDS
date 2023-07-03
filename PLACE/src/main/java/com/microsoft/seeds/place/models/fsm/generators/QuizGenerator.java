package com.microsoft.seeds.place.models.fsm.generators;

import com.microsoft.seeds.place.models.fsm.*;
import com.microsoft.seeds.place.models.fsm.actions.*;
import com.microsoft.seeds.place.models.request.CreateQuizRequest;
import com.microsoft.seeds.place.models.utils.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class QuizGenerator extends FSMGeneratorAPI {

    public static ExpFSM getFSMFromOnDemandFSMData(OnDemandFSMData onDemandFSMData){
        CreateQuizRequest createQuizRequest = CreateQuizRequest.fromJSON(onDemandFSMData.data);
        System.out.println("SHUFFLING THE QUIZ!");
        List<Integer> indexArray = IntStream.range(0, createQuizRequest.questions.size())
                .boxed().collect(Collectors.toList());
        Collections.shuffle(indexArray);
        List<String> questions = new ArrayList<>();
        List<List<String>> options  = new ArrayList<>();
        List<Integer> answers = new ArrayList<>();
        for (int index : indexArray) {
            questions.add(createQuizRequest.questions.get(index));

            int currAnsIndex = createQuizRequest.correctAnswers.get(index);
            List<Integer> optionsIndexArray = IntStream.range(0, createQuizRequest.options.get(index).size())
                    .boxed().collect(Collectors.toList());
            Collections.shuffle(optionsIndexArray);
            List<String> shuffledOptions = new ArrayList<>();
            int answer = -1;
            for (int k = 0; k < optionsIndexArray.size(); ++k) {
                int optionIndex = optionsIndexArray.get(k);
                shuffledOptions.add(createQuizRequest.options.get(index).get(optionIndex));
                if (optionIndex == currAnsIndex) {
                    answer = k;
                }
            }

            options.add(shuffledOptions);
            answers.add(answer);
        }

        createQuizRequest.setQuestions(questions);
        createQuizRequest.setOptions(options);
        createQuizRequest.setCorrectAnswers(answers);

        return getFSM(createQuizRequest, onDemandFSMData.version);
    }
    public static ExpFSM getFSM(CreateQuizRequest quizRequest, int version){
        if(!quizRequest.isValid()){
            System.out.println(" RETURNING NULL FROM INVALID QUIZ REQUEST ");
            return null;
        }
        ExpFSM quiz = new ExpFSM(quizRequest.getId(), version, Constants.QUIZ_FSM_TYPE, quizRequest.language);
        quiz.setInitAction(new SkipAction());
        List<JSONObject> stateData = new ArrayList<>();
        quizRequest.getQuestionsAudioFileWithSpeechRates().forEach(questionAudioFileWithSpeechRate -> stateData.add(getStateData(Collections.singletonList(questionAudioFileWithSpeechRate))));
        FSMActionList entryStateAction = new FSMActionList()
                .add(new StdOutAction())
                .add(new PushFSMStateAction());
        FSMAction entryScoreStateAction = new FSMActionList()
                .add(new StdOutAction())
                .add(new PushFSMStateAction())
                .add(new AutoEventDispatchAction());
        FSMAction entryEndStateAction = new FSMActionList()
                .add(new PushFSMStateAction())
                .add(new PushFSMContextDataAction())
//                .add(new StopAction())
                .add(new AutoPopFSMAction());
        int i = 0;

        QuizStaticDialogs quizStaticDialogs = new QuizStaticDialogs(quizRequest.language);
        List<AudioFileWithSpeechRates> rightAnswerPrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.congratulations));
        List<AudioFileWithSpeechRates> wrongAnswerPrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.wrongAnswer));
        List<AudioFileWithSpeechRates> yourScorePrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.yourScoreIs));
        List<AudioFileWithSpeechRates> movingOnToNextPrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.movingOnToNextQuestion));
        List<AudioFileWithSpeechRates> finalScorePrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.finalScoreIs));
        List<AudioFileWithSpeechRates> thanksForParticipatingPrompt = quizRequest.getAudioFileWithSpeechRatesList(Collections.singletonList(quizStaticDialogs.thanksForParticipating));
        List<AudioFileWithSpeechRates> currScorePlaceHolderPrompt = Collections.singletonList(PlaceHolderAudioFileWithSpeechRates
                .getFor(PlaceHolderAudioFileWithSpeechRates.Type.numerical, Constants.QUIZ_SCORE_KEY, quizRequest.speechRates));

        List<AudioFileWithSpeechRates> correctAnswer = Stream.of(
                        rightAnswerPrompt, yourScorePrompt, currScorePlaceHolderPrompt, movingOnToNextPrompt)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<AudioFileWithSpeechRates> wrongAnswer = Stream.of(
                        wrongAnswerPrompt, yourScorePrompt, currScorePlaceHolderPrompt, movingOnToNextPrompt)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<AudioFileWithSpeechRates> finalScore = Stream.of(
                        finalScorePrompt, currScorePlaceHolderPrompt, thanksForParticipatingPrompt)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

//        createEvent(new Event(Constants.TIMEOUT_EVENT, null), quiz);
        // CREATE QUESTION AND SCORE STATES
        for(int k = 0; k<stateData.size(); ++k){
            JSONObject data = stateData.get(k);
            createState(entryStateAction, ++i, data, new SkipAction(), ExpectedInputType.EVENT_NAME, quiz);
//            createTransition(questionStateId, questionStateId, Constants.TIMEOUT_EVENT, new SkipAction(), quiz);

            if(k < stateData.size() - 1) {
                createState(entryScoreStateAction, ++i, getStateData(correctAnswer), new SkipAction(), ExpectedInputType.NIL, quiz);
                createState(entryScoreStateAction, ++i, getStateData(wrongAnswer), new SkipAction(), ExpectedInputType.NIL, quiz);
            }else{
                createState(entryScoreStateAction, ++i, getStateData(rightAnswerPrompt), new SkipAction(), ExpectedInputType.NIL, quiz);
                createState(entryScoreStateAction, ++i, getStateData(wrongAnswerPrompt), new SkipAction(), ExpectedInputType.NIL, quiz);
            }
        }

        int endStateId = ++i;
        createState(entryEndStateAction, endStateId,
                getStateData(finalScore), null, ExpectedInputType.NIL, quiz);
        quiz.setFirstState(1);

        for(int questionNum = 0; questionNum< quizRequest.options.size(); ++questionNum){
            AtomicInteger optionNum = new AtomicInteger();
            int finalQuestionNum = questionNum;
            quizRequest.getOptionAudioFileWithSpeechRatesFor(questionNum).forEach(optionAudioData -> {
                createEvent(new Event(getEventName(finalQuestionNum, optionNum.get()),
                        Collections.singletonList(optionAudioData), new SkipAction()),quiz);
                optionNum.getAndIncrement();
            });
        }

        createEvent(new Event(Constants.AUTO_EVENT, null), quiz);

//        createOpsEvent(Constants.EXIT_EVENT, FSMActionList.getExitActionList(), quiz);

        List<Integer> answers = quizRequest.getCorrectAnswers();
        FSMAction correctAnswerAction = new IncrementVariableAction()
                .setVariableName(Constants.QUIZ_SCORE_KEY)
                .setIncrementBy(quizRequest.getPositiveMark());
        FSMAction incorrectAnswerAction = new DecrementVariableAction()
                .setVariableName(Constants.QUIZ_SCORE_KEY)
                .setDecrementBy(quizRequest.getNegativeMark());

        int questionNum = 0;
        for(int questionStateId = 1; questionStateId<=stateData.size() * 3 - 2; questionStateId+=3){
            createTransition(questionStateId + 1, questionStateId + 3, Constants.AUTO_EVENT, new SkipAction(), quiz); // FROM CORRECT ANSWER SCORE STATE TO NEXT QUESTION STATE
            createTransition(questionStateId + 2, questionStateId + 3, Constants.AUTO_EVENT, new SkipAction(), quiz); // FROM WRONG ANSWER SCORE STATE TO NEXT QUESTION STATE

            int optionsSize = quizRequest.options.get(questionStateId/3).size();
            List<String> eventsOnThisState = new ArrayList<>();
            for(int optionNum = 0; optionNum<optionsSize; ++optionNum){
                eventsOnThisState.add(getEventName(questionNum, optionNum));
            }
            int answerIndex = answers.get(questionStateId/3);
            if(answerIndex >= 0 && answerIndex < eventsOnThisState.size()) {
                for (int k = 0; k < eventsOnThisState.size(); ++k) {
                    boolean isCorrectAns = k == answerIndex;
                    createTransition(questionStateId, isCorrectAns ? questionStateId + 1 : questionStateId + 2,
                            eventsOnThisState.get(k), isCorrectAns ? correctAnswerAction : incorrectAnswerAction, quiz);
                }
            }else{
                System.out.println(" RETURNING NULL FROM ANSWER INDEX CHECK FOR QUIZ ");
                return null;
            }
            questionNum++;
        }
        return quiz;
    }

    public static List<ExpFSM> getDummyFSMs(){
        List<ExpFSM> res = new ArrayList<>();
        for(CreateQuizRequest quizReq : CreateQuizRequest.getListOfDummyRequests()){
            res.add(getFSM(quizReq, 1));
        }
        return res;
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

    private static String getEventName(int questionNum, int optionNum){
        return "questionNum-" + questionNum + "-optionNum-" + optionNum;
    }

}
