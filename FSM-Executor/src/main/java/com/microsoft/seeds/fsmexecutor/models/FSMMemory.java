package com.microsoft.seeds.fsmexecutor.models;


import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.generators.QuizGenerator;
import com.microsoft.seeds.fsmexecutor.models.fsm.generators.StoryGenerator;
import com.microsoft.seeds.fsmexecutor.models.request.CreateQuizRequest;
import com.microsoft.seeds.fsmexecutor.models.request.CreateStoryRequest;

import java.util.*;

public class FSMMemory {
    private static FSMMemory instance;
    private Hashtable<String, ExpFSM> fsms;

    private FSMMemory() {
        fsms = new Hashtable<>();
        initialiseDummyDatabase();
    }

    public void addFSM(String name, ExpFSM fsm){
        fsms.put(name, fsm);
    }

    public ExpFSM getFSM(String name){
        return fsms.get(name);
    }

    public static FSMMemory getInstance(){
        if(instance == null){
            instance = new FSMMemory();
        }
        return instance;
    }

    public Hashtable<String, List<String>> getAllFSM(){
        Hashtable<String, List<String>> res = new Hashtable<>();
        Enumeration<ExpFSM> fsmEnumeration = fsms.elements();
        while(fsmEnumeration.hasMoreElements()){
           ExpFSM fsm = fsmEnumeration.nextElement();
           if(res.containsKey(fsm.getType())){
               List<String> fsmIds = res.get(fsm.getType());
               fsmIds.add(fsm.getId());
               res.put(fsm.getType(), fsmIds);
           }else{
               res.put(fsm.getType(), new ArrayList<>(Collections.singletonList(fsm.getId())));
           }
        }
        return res;
    }

    private void initialiseDummyDatabase(){
//        List<CreateQuizRequest> quizRequestList = CreateQuizRequest.getListOfDummyRequests();
//        for(int i = 0; i< quizRequestList.size(); ++i){
//            ExpFSM fsm = QuizGenerator.getFSM(quizRequestList.get(i));
//            addFSM(fsm.getId(), fsm);
//        }
//        List<CreateStoryRequest> storyList = CreateStoryRequest.getListOfDummyRequests();
//        for(int i = 0; i< storyList.size(); ++i){
//            ExpFSM fsm = StoryGenerator.getFSM(storyList.get(i));
//            addFSM(fsm.getId(), fsm);
//        }
    }
}
