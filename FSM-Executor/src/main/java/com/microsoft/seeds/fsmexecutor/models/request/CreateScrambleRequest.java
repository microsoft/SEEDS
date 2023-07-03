package com.microsoft.seeds.fsmexecutor.models.request;

import com.microsoft.seeds.fsmexecutor.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.fsmexecutor.models.utils.ExpectedInputType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;

public class CreateScrambleRequest {
    public static List<Double> speechRates;

    public String id;
    public String fullAudio;
    public List<String> lines;
    public String language;


    public CreateScrambleRequest() {
        speechRates = new ArrayList<>(Arrays.asList(0.5, 0.75, 1.0, 1.5, 2.0));
    }

    public CreateScrambleRequest(String id, String language, String fullAudio, List<String> lines) {
        this.id = id;
        this.fullAudio = fullAudio;
        this.lines = lines;
        this.language = language;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullAudio() {
        return fullAudio;
    }

    public void setFullAudio(String fullAudio) {
        this.fullAudio = fullAudio;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Double> getSpeechRates() {
        return speechRates;
    }

    public void setSpeechRates(List<Double> speechRates) {
        this.speechRates = speechRates;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("fullAudio", fullAudio);
        res.put("lines", lines);
        res.put("language", language);
        return res;
    }

    public static List<AudioFileWithSpeechRates> getAudioFileDescriptionWithSpeechRates(List<String> paths){
        List<AudioFileWithSpeechRates> audios = new ArrayList<>();
        for(String path: paths) {
            audios.add(AudioFileWithSpeechRates.getFor(path));
        }
        return audios;
    }

    public List<AudioFileWithSpeechRates> getLinesAudioFileDescriptions(){
        return getAudioFileDescriptionWithSpeechRates(lines);
    }

    public List<AudioFileWithSpeechRates> getFullAudioFileDescription(String pressAnyKeyBetweenPrompt){
        return getAudioFileDescriptionWithSpeechRates(new ArrayList<>(Arrays.asList(fullAudio, pressAnyKeyBetweenPrompt)));
    }



    public static CreateScrambleRequest fromJSON(JSONObject json){
        return new
                CreateScrambleRequest(
                json.getString("id"),
                json.getString("language"),
                json.getString("fullAudio"),
                json
                        .getJSONArray("lines")
                        .toList()
                        .stream()
                        .map(item -> String.valueOf(item))
                        .collect(Collectors.toList()));
    }

    public boolean isValid(){
        return id != null && fullAudio != null && language != null && lines != null && !lines.isEmpty();
    }

    public static CreateScrambleRequest getDummyRequest(){
        String id = "Johnny Johnny";
        String fullAudioPath = "output-container/23";
        List<String> lines = new ArrayList<>(Arrays.asList("output-container/23_1", "output-container/23_2", "output-container/23_3", "output-container/23_4"));
        String language = Constants.ENGLISH_LANG;
        return new CreateScrambleRequest(id, language, fullAudioPath, lines);
    }

    public ExpectedInputType getExpectedInputTypeForAnswerCheckState(){
        switch (lines.size()){
            case 1:
                return ExpectedInputType.ONE_DIGIT;
            case 2:
                return ExpectedInputType.TWO_DIGIT;
            case 3:
                return ExpectedInputType.THREE_DIGIT;
            case 4:
                return ExpectedInputType.FOUR_DIGIT;
            case 5:
                return ExpectedInputType.FIVE_DIGIT;
            case 6:
                return ExpectedInputType.SIX_DIGIT;
            case 7:
                return ExpectedInputType.SEVEN_DIGIT;
            case 8:
                return ExpectedInputType.EIGHT_DIGIT;
            case 9:
                return ExpectedInputType.NINE_DIGIT;
            default:
                return ExpectedInputType.NIL;
        }
    }
}
