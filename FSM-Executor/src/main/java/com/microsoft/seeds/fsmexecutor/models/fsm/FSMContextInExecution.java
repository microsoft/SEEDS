package com.microsoft.seeds.fsmexecutor.models.fsm;

import java.util.Enumeration;
import java.util.Hashtable;

public class FSMContextInExecution {

    private final long MAX_SIZE = 50;
    private static FSMContextInExecution instance;
    private Hashtable<String, FSMContext> fsmContextHashtable;

    private FSMContextInExecution(){
        fsmContextHashtable = new Hashtable<>();
    }

    public static FSMContextInExecution getInstance(){
        if(instance == null){
            instance = new FSMContextInExecution();
        }
        return instance;
    }

    public void add(FSMContext fsmContext){
        if(fsmContextHashtable.size() == MAX_SIZE){
            throw new RuntimeException("RUNNING FSM CONTEXT MAX NUMBER REACHED!");
        }
        fsmContextHashtable.put(fsmContext.getId(), fsmContext);
    }

    public boolean canAdd(){
        return fsmContextHashtable.size() < MAX_SIZE;
    }

    public FSMContext get(String fsmId){
        return fsmContextHashtable.get(fsmId);
    }

    public boolean isPresent(String fsmId){
        return fsmContextHashtable.containsKey(fsmId);
    }

    public void remove(String fsmId){
        fsmContextHashtable.remove(fsmId);
    }

    public void removeAll(){
        fsmContextHashtable.clear();
    }

    public void printFSMIDs(){
        Enumeration<String> fsmIdEnum = fsmContextHashtable.keys();
        while(fsmIdEnum.hasMoreElements()){
            System.out.println(fsmIdEnum.nextElement());
        }
    }
}
