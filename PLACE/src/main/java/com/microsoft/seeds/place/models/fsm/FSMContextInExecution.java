package com.microsoft.seeds.place.models.fsm;

import java.util.Enumeration;
import java.util.Hashtable;

public class FSMContextInExecution {
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
        fsmContextHashtable.put(fsmContext.getId(), fsmContext);
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
