package com.microsoft.seeds.place.models.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class QuizSummary {
    public String id;
    public String language;
    public String title;
    public boolean isProcessed;

    public AudioFileWithSpeechRates titleAudio;

    public QuizSummary(String id, String title, String language, boolean isProcessed) {
        this.id = id;
        this.language = language;
        this.title = title;
        this.isProcessed = isProcessed;
    }

    public QuizSummary(String id, String title, String language, boolean isProcessed, AudioFileWithSpeechRates titleAudio) {
        this.id = id;
        this.language = language;
        this.title = title;
        this.isProcessed = isProcessed;
        this.titleAudio = titleAudio;
    }

    // FOR PULLMODEL MENU
    public QuizSummary(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public AudioFileWithSpeechRates getTitleAudio() {
        return titleAudio;
    }

    public void setTitleAudio(AudioFileWithSpeechRates titleAudio) {
        this.titleAudio = titleAudio;
    }

    public QuizSummary() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("language", language);
        res.put("title", title);
        res.put("isProcessed", isProcessed);
        if(titleAudio != null)
            res.put("titleAudio", titleAudio.toJSON());
        return res;
    }

    public static JSONArray getJSONArray(List<QuizSummary> quizSummaryList){
        JSONArray jsonArray = new JSONArray();
        for (QuizSummary quizSummary : quizSummaryList) {
            jsonArray.put(quizSummary.toJSON());
        }
        return jsonArray;
    }
}
