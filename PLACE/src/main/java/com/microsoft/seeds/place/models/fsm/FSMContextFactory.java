package com.microsoft.seeds.place.models.fsm;

import com.microsoft.seeds.place.models.fsm.generators.QuizGenerator;
import com.microsoft.seeds.place.models.utils.Constants;

public class FSMContextFactory {
    public static FSMContext getFSMContext(ExpFSM fsm, String id){
        return fsm.createFSMContext(id);
    }

    public static FSMContext getFSMContext(OnDemandFSMData onDemandFSMData, String id){
        switch (onDemandFSMData.type){
            case Constants.QUIZ_FSM_TYPE:
                return getFSMContext(QuizGenerator.getFSMFromOnDemandFSMData(onDemandFSMData), id);
            default:
                return null;
        }
    }
}
