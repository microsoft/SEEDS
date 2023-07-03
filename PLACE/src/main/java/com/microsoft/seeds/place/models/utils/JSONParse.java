package com.microsoft.seeds.place.models.utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JSONParse {
    private static final Logger logger = Logger.getLogger(JSONParse.class.getName());
    public static List<List<String>> get2DStringList(JSONArray rows){
        List<List<String>> res = new ArrayList<>();
        try {
            int count = rows.length();
            for (int i = 0; i < count; i++) {
                JSONArray jsonArr = rows.getJSONArray(i);
                res.add(new ArrayList<>());
                for (Object o : jsonArr) {
                    res.get(i).add(o.toString());
                }
            }
        } catch (JSONException e) {
            logger.severe("JSON PARSING ERROR : " + rows.toString());
        }
        return res;
    }

    public static List<String> getStringList(JSONArray rows){
        List<String> res = new ArrayList<>();
        try {
            int count = rows.length();
            for (int i = 0; i < count; i++) {
                res.add(rows.getString(i));
            }
        } catch (JSONException e) {
            logger.severe("JSON PARSING ERROR : " + rows.toString());
        }
        return res;
    }

    public static List<Integer> getIntList(JSONArray rows){
        List<Integer> res = new ArrayList<>();
        try {
            int count = rows.length();
            for (int i = 0; i < count; i++) {
                res.add(rows.getInt(i));
            }
        } catch (JSONException e) {
            logger.severe("JSON PARSING ERROR : " + rows.toString());
        }
        return res;
    }

    public static List<Double> getDoubleList(JSONArray rows){
        return rows.toList()
                .stream()
                .map(item -> Double.parseDouble(String.valueOf(item)))
                .collect(Collectors.toList());
    }

    public static JSONArray getJSONArrayFromDoubleList(List<Double> lis){
        return new JSONArray(lis.stream().map(item -> String.valueOf(item)).collect(Collectors.toList()));
    }

}
