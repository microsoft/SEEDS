package com.microsoft.seeds.place.models.utils;

import org.json.JSONObject;

public class RawDataSummary {
    private String id;
    private String title;
    private String type;
    private String experienceLanguage;

    public RawDataSummary(String id, String title, String type, String experienceLanguage) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.experienceLanguage = experienceLanguage;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("title", title);
        res.put("type", type);
        res.put("experienceLanguage", experienceLanguage);
        return res;
    }
}
