package com.microsoft.seeds.fsmexecutor.models.utils;

public class StoryLine {
    private String text;
    private double time;

    public StoryLine(String text, double time) {
        this.text = text;
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public double getTime() {
        return time;
    }
}
