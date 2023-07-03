package com.microsoft.seeds.place.models.utils;

import com.microsoft.seeds.place.models.fsm.FSMContext;

public interface OnCompleteValidation {
    boolean check(FSMContext fsmc);
}
