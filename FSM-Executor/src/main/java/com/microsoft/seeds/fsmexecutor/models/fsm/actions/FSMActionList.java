package com.microsoft.seeds.fsmexecutor.models.fsm.actions;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FSMActionList implements FSMAction
{
    Vector actions = new Vector();
    public FSMActionList add(FSMAction a)
    {
        FSMActionList cpy = new FSMActionList();
        cpy.getAction().addAll(this.actions);
        cpy.getAction().add(a);
        return cpy;
    }

    public Vector getAction(){
        return actions;
    }

    public void execute(FSMContext fsmc, Object data)
    {
        for(int i=0 ; i<actions.size() ; i++)
        {
            FSMAction action = (FSMAction) actions.elementAt(i);
            action.execute(fsmc, data);
        }
    }

    @Override
    public List<String> getActionName() {
        List<String> res = new ArrayList<>();
        for(int i=0 ; i<actions.size() ; i++)
        {
            res.addAll(((FSMAction)actions.elementAt(i)).getActionName());
        }
        return res;
    }

    @Override
    public FSMAction getInstanceFromArgs(JSONObject args) {
        throw new NotImplementedException();
    }

    @Override
    public JSONArray getInstanceArgs() {
        JSONArray array = new JSONArray();
        actions.forEach(action -> {
            FSMAction fsmAction = (FSMAction) action;
            array.put(fsmAction.getInstanceArgs().get(0));
        });
        return array;
    }

    public static FSMActionList getExitActionList(){
        return  new FSMActionList()
        .add(new StopAction())
        .add(new PushFSMContextDataAction())
        .add(new AutoPopFSMAction());
    }

    public static FSMActionList getAbortActionList(){
        return new FSMActionList()
                .add(new StopAction())
                .add(new PushFSMContextDataAction());
    }

}

