package com.microsoft.seeds.place.models.utils;

import com.microsoft.seeds.place.models.fsm.FSMContext;
import org.json.JSONObject;

import java.util.List;

public class PlaceHolderAudioFileWithSpeechRates {
    private static final String fakeContainer = "fakeContainer/";
    public enum Type {
        numerical
    }

    public static AudioFileWithSpeechRates getFor(Type type, String valueKey, List<Double> speechRate){
        return AudioFileWithSpeechRates.getFor(fakeContainer + type.name() + "/" + valueKey, speechRate);
    }

    public static AudioFileWithSpeechRates getFor(Type type, String valueKey){
        return AudioFileWithSpeechRates.getFor(fakeContainer + type.name() + "/" + valueKey);
    }

    public static AudioFileWithSpeechRates getRealFor(JSONObject toReplaceAudioFileWithSpeechRate, FSMContext fsmc){
        String language = fsmc.getLanguage();
        String[] basePathParts = AudioFileWithSpeechRates.fromJSON(toReplaceAudioFileWithSpeechRate).getBasePath().split("/");
        Type type = Type.valueOf(basePathParts[0]);
        String valueKey = basePathParts[1];
        switch (type){
            default:
                return AudioFileWithSpeechRates.getFor(NumberDialogs.getPathFor(language, String.valueOf(fsmc.get(valueKey))));
        }
    }

    public static AudioFileWithSpeechRates getRealFor(JSONObject toReplaceAudioFileWithSpeechRate, List<Double> speechRate, FSMContext fsmc){
        String language = fsmc.getLanguage();
        String[] basePathParts = AudioFileWithSpeechRates.fromJSON(toReplaceAudioFileWithSpeechRate).getBasePath().split("/");
        Type type = Type.valueOf(basePathParts[0]);
        String valueKey = basePathParts[1];
        switch (type){
            default:
                return AudioFileWithSpeechRates.getFor(NumberDialogs.getPathFor(language, String.valueOf(fsmc.get(valueKey))), speechRate);
        }
    }

    public static boolean isPlaceHolder(JSONObject audioFileWithSpeechRatesJSON){
        AudioFileWithSpeechRates audioFileWithSpeechRates = AudioFileWithSpeechRates.fromJSON(audioFileWithSpeechRatesJSON);
        try{
            Type.valueOf(audioFileWithSpeechRates.getBasePath().split("/")[0]);
            return true;
        }catch (Exception ex) {
            return false;
        }
    }
}
