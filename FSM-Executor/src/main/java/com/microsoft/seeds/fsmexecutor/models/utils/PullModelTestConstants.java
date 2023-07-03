package com.microsoft.seeds.fsmexecutor.models.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PullModelTestConstants {
    public static final String NO_INPUT = "NIL";
    public static final List<String> INPUTS = new ArrayList<>(Arrays.asList("NIL", "story", "NEXT", "NEXT",
            "GO_BACK", "SELECT", "finished_audio", "NIL", "GO_TO_PREV_MENU", "REPEAT_MENU", "song", "NEXT", "SELECT", "POP_FSM", "AbortExperience"));

    public static final List<String> INPUTS_TEST_2 = new ArrayList<>(Arrays.asList("NIL", "story", "GO_BACK", "SELECT", "NEXT",
            "GO_BACK", "SELECT", "finished_audio", "NIL", "GO_TO_PREV_MENU", "REPEAT_MENU", "song", "NEXT", "SELECT", "POP_FSM", "AbortExperience"));

    public static final List<String> INPUTS_QUIZ = new ArrayList<>(Arrays.asList("NIL", "quiz", "NEXT", "NEXT", "SELECT",
            "questionNum-0-optionNum-0",
            "questionNum-1-optionNum-0",
            "questionNum-2-optionNum-0",
            "questionNum-3-optionNum-0",
            "questionNum-4-optionNum-0",
            "questionNum-5-optionNum-0",
            "questionNum-6-optionNum-0",
            "questionNum-7-optionNum-0",
            "questionNum-8-optionNum-0",
            "questionNum-9-optionNum-0",
            "GO_TO_PREV_MENU","AbortExperience"));

    public static final List<String> INPUTS_TEST = new ArrayList<>(Arrays.asList("NIL", "story", "NEXT", "AbortExperience"));
}
