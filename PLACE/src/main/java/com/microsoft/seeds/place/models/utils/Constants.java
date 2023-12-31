package com.microsoft.seeds.place.models.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String FSM_TYPE_STATIC = "static";
    public static final String FSM_TYPE_ON_DEMAND = "onDemand";
    public static final String FSM_TYPE_KEY = "fsmType";
    public static final int FSM_CACHE_SIZE = 100;

    public static final String AUDIO_DATA_KEY = "audioData";

    public static final int ON_DEMAND_FSM_DATA_CACHE_SIZE = 100;
    public static final int START_FSM_REQ_QUEUE_TIMEOUT = 30000;
    public static final String QUIZ_SCORE_KEY = "score";
    public static final String AUTO_EVENT = "auto";
    public static final String STATE_DATA_TEXT_KEY = "text";
    public static final String PULL_MODEL_PREVIOUS_MENU_EVENT = "Previous Menu";
    public static final String PULL_MODEL_REPEAT_MENU_EVENT = "Repeat Menu";
    public static final String PULL_MODEL_FSM_TYPE = "Pull Model";
    public static final String PULL_MODEL_MENU_ITEM_MESSAGE_TYPE = "pullModelMenuItem";
    public static final String SCRAMBLE_CORRECT_ORDER_LINES_KEY = "correctOrderLines";
    public static final String SCRAMBLE_KEY_MAPPINGS_KEY_PREFIX = "mapping_";
    public static final String STORY_FSM_TYPE = "story";
    public static final String SONGS_FSM_TYPE = "song";
    public static final String RHYMES_FSM_TYPE = "rhyme";
    public static final String POEM_FSM_TYPE = "poem";
    public static final String SPEECH_FSM_TYPE = "speech";
    public static final String QUIZ_FSM_TYPE = "quiz";
    public static final String SCRAMBLE_FSM_TYPE = "scramble";

    public static final String STORY_PAUSE_EVENT = "pause";
    public static final String STORY_PLAY_EVENT = "play";
    public static final String EXIT_EVENT = "exit";
    public static final String ABORT_EVENT = "AbortExperience";
    public static final String TIMEOUT_EVENT = "Timeout";

    public static final String ENGLISH_LANG = "English";
    public static final String KANNADA_LANG = "Kannada";
    public static final List<Double> AUDIO_FILE_SPEECH_RATES = new ArrayList<>(Arrays.asList(0.5, 0.75, 1.0, 1.5, 2.0));
    public static final String AUDIO_FILE_EXTENSION = "mp3";
    public static final String VERSION_DESCRIPTION = "Flat Pull Model";
    public static final String VERSION_PLACE = "v2.0.0";
}
