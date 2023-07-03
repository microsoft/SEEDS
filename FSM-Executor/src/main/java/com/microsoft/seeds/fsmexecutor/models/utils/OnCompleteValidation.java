package com.microsoft.seeds.fsmexecutor.models.utils;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;

public interface OnCompleteValidation {
    boolean check(FSMContext fsmc);
}
