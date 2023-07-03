package com.microsoft.seeds.fsmexecutor.models.fsm.generators;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.*;

public class ComposableFSMExecutorGenerator extends FSMGeneratorAPI {
//    public static ComposableFSMContextExecutor getComposableFSMContextExecutor(CreatePullModelRequest createPullModelRequest){
//        // ASSUMING ALL FSMs are PRESENT IN FSMMemory
//        ExpFSM fsm = new ExpFSM("Pull Model Menu", 1, Constants.PULL_MODEL_FSM_TYPE, "English");
//        fsm.setInitAction(new SkipAction());
//        FSMAction entryAction = new FSMActionList()
//                .add(new StdOutAction())
//                .add(new PushFSMStateAction());
//
//        createEvent(Constants.PULL_MODEL_PREVIOUS_MENU_EVENT, new SkipAction(), fsm);
//        createEvent(Constants.PULL_MODEL_REPEAT_MENU_EVENT, new SkipAction(), fsm);
//        createOpsEvent(Constants.EXIT_EVENT, FSMActionList.getExitActionList(), fsm);
//
//        createState(entryAction, 1, null, new SkipAction(), fsm);
//        createTransition(1, 1, Constants.PULL_MODEL_REPEAT_MENU_EVENT, new SkipAction(), fsm);
//        fsm.setFirstState(1);
//        Set<String> fsmTypes = createPullModelRequest.availableFSM.keySet();
//        int typeIndex = 1;
//        for(String type: fsmTypes){
//            createEvent(type, new SkipAction(), fsm);
//            createState(entryAction, typeIndex + 1, null, new SkipAction(), fsm);
//            createTransition(1, typeIndex + 1, type, new SkipAction(), fsm);
//            createTransition(typeIndex + 1, 1, Constants.PULL_MODEL_PREVIOUS_MENU_EVENT, new SkipAction(), fsm);
//            createTransition(typeIndex + 1, typeIndex + 1, Constants.PULL_MODEL_REPEAT_MENU_EVENT, new SkipAction(), fsm);
//            for(String itemName: createPullModelRequest.availableFSM.get(type)){
//                createEvent(itemName, new SkipAction(), fsm);
//                createFSMTransition(itemName, fsm, typeIndex + 1, FSMMemory.getInstance().getFSM(itemName), "abcd", new SkipAction());
//            }
//            typeIndex++;
//        }
//        return new ComposableFSMContextExecutor(createPullModelRequest.id, fsm.createFSMContext(createPullModelRequest.id),
//                createPullModelRequest.webClientBuilder, createPullModelRequest.clientEp, createPullModelRequest.userInputEp);
//    }
//
//    private static ExpFSM getDummyMenuFSM(){
//        ExpFSM fsm = new ExpFSM("dummy menu", 1, "menu", "English");
//        fsm.setInitAction(new SkipAction());
//        FSMAction entryAction = new FSMActionList()
//                .add(new StdOutAction())
//                .add(new PushFSMStateAction());
//        FSMAction entryActionEndState = new FSMActionList()
//                .add(new PushFSMStateAction())
//                .add(new PushFSMContextDataAction())
//                .add(new StopAction());
//        createState(entryAction, 1, getSingleStringStateData("Choose your language"), new SkipAction(), fsm);
//        createState(entryAction, 2, getSingleStringStateData("Choose your favourite experience"), new SkipAction(), fsm);
//        createState(entryActionEndState, 3, null, null, fsm);
//        fsm.setFirstState(1);
//        createEvent("English", new SkipAction(), fsm);
//        createEvent("Hindi", new SkipAction(), fsm);
//        createEvent("Kannada", new SkipAction(), fsm);
//
//        createEvent("Dummy Quiz", new SkipAction(), fsm);
//        createEvent("Dummy Story", new SkipAction(), fsm);
//        createEvent("Exit", new SkipAction(), fsm);
//
//        createTransition(1, 2, "English", new SkipAction(), fsm);
//        createTransition(1, 2, "Hindi", new SkipAction(), fsm);
//        createTransition(1, 2, "Kannada", new SkipAction(), fsm);
//        createTransition(1, 3, "Exit", new SkipAction(), fsm);
//
//        createFSMTransition("Dummy Quiz", fsm, 2,
//                QuizGenerator.getFSM(CreateQuizRequest.getDummyQuizRequest(), 1), "abcd1", new SkipAction());
//        createFSMTransition("Dummy Story", fsm, 2,
//                StoryGenerator.getFSM(CreateStoryRequest.getDummyStoryRequest()), "abcd2", new SkipAction());
//        createTransition(2, 3, "Exit", new SkipAction(), fsm);
//
//        return fsm;
//    }
//
//    public static ComposableFSMContextExecutor getDummyComposableFSMContextExecutor(WebClient.Builder webClientBuilder, String clientEp, String userInputEp){
//        return new ComposableFSMContextExecutor(ComposableFSMContextExecutor.DUMMY_COMPOSABLE_ID, getDummyMenuFSM().createFSMContext("abcd"),
//                webClientBuilder, clientEp, userInputEp);
//    }
}
