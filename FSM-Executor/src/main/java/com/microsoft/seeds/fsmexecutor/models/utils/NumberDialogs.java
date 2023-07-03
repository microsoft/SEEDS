package com.microsoft.seeds.fsmexecutor.models.utils;

import java.util.HashMap;
import java.util.Map;

public class NumberDialogs {
    private static String basePath = "https://seedsblob.blob.core.windows.net/pull-model-menus/keys/${lang}/${num}/${num}";

    public static String getPathFor(String language, String num){
        Map<String, String> replacementMap  = new HashMap<String, String>() {{
            put("lang", language.toLowerCase());
            put("num", num);
        }};
        return StringInterpolator.substituteForMap(basePath, replacementMap);
    }
}
