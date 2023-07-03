package com.microsoft.seeds.fsmexecutor.models.utils;

import com.microsoft.seeds.fsmexecutor.models.fsm.FSMContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringInterpolator {
    public static String START_PATTERN = "\\$\\{";
    public static String END_PATTERN = "\\}";

    private static Pattern MY_PATTERN = Pattern.compile(START_PATTERN +"(.*?)"+ END_PATTERN);

    public static String getInterpolatedString(String data, FSMContext fsmc){
        Matcher m = MY_PATTERN.matcher(data);
        String finalStr = data;
        while (m.find()) {
            String s = m.group(1);
            finalStr = finalStr.replaceFirst(START_PATTERN + s + END_PATTERN, String.valueOf(fsmc.get(s.trim())));
        }
        return finalStr;
    }

    public static String substituteForMap(String data, Map<String, String> replacements){
        Matcher m = MY_PATTERN.matcher(data);
        String finalStr = data;
        while (m.find()) {
            String s = m.group(1);
            if(replacements.containsKey(s)) {
                finalStr = finalStr.replaceFirst(START_PATTERN + s + END_PATTERN, replacements.get(s));
            }
        }
        return finalStr;
    }
}
