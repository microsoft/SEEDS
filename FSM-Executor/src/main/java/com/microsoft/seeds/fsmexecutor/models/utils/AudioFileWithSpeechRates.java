package com.microsoft.seeds.fsmexecutor.models.utils;

import org.json.JSONObject;

import java.util.*;

public class AudioFileWithSpeechRates {
    public List<AudioFile> audioFileList;

    public AudioFileWithSpeechRates(List<AudioFile> audioFileList) {
        this.audioFileList = audioFileList;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        audioFileList.forEach(audioFile ->
                res.put(String.valueOf(audioFile.speechRate), audioFile.getJSONWithoutSpeechRate()));
        return res;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> res = new HashMap<>();
        for (AudioFile audioFile : audioFileList) {
            res.put(String.valueOf(audioFile.speechRate), audioFile.getMapWithoutSpeechRate());
        }
        return res;
    }

    public static AudioFileWithSpeechRates fromJSON(JSONObject json){
        List<AudioFile> audioFiles = new ArrayList<>();
        for(String speechRateStr : json.keySet()){
            audioFiles.add(AudioFile.getInstanceFromJSON(json.getJSONObject(speechRateStr), Double.parseDouble(speechRateStr)));
        }
        return new AudioFileWithSpeechRates(audioFiles);
    }

    public static AudioFileWithSpeechRates getFor(String path){
        List<AudioFile> res = new ArrayList<>();
        for (double speechRate : Constants.AUDIO_FILE_SPEECH_RATES) {
            String fileName = path + "/" + speechRate + "." + Constants.AUDIO_FILE_EXTENSION;
            res.add(new AudioFile(fileName, 0, speechRate));
        }
        return new AudioFileWithSpeechRates(res);
    }

    public static AudioFileWithSpeechRates getFor(String path, String extension){
        List<AudioFile> res = new ArrayList<>();
        for (double speechRate : Constants.AUDIO_FILE_SPEECH_RATES) {
            String fileName = path + "/" + speechRate + "." + extension;
            res.add(new AudioFile(fileName, 0, speechRate));
        }
        return new AudioFileWithSpeechRates(res);
    }

    public static AudioFileWithSpeechRates getFor(String path, List<Double> speechRates){
        List<AudioFile> res = new ArrayList<>();
        for (double speechRate : speechRates) {
            String fileName = path + "/" + speechRate + "." + Constants.AUDIO_FILE_EXTENSION;
            res.add(new AudioFile(fileName, 0, speechRate));
        }
        return new AudioFileWithSpeechRates(res);
    }

    public String getBasePath(){
        String path = audioFileList.get(0).getFilePath();
        String[] parts = path.split("/");
        return String.join("/", Arrays.copyOfRange(parts, 0, parts.length-1));
    }
}
