package com.microsoft.seeds.place.models.request;

import com.microsoft.seeds.place.models.utils.AudioFile;
import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.place.models.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/*
*     {
        "_id": "63e4e8a52a2b5ba58aed8c4f",
        "title": "Old Woman and four thieves1",
        "description": "A story about how an old woman transforms the mind of thieves who have a plan to steal a treasure",
        "id": "1009",
        "type": "Rhyme",
        "language": "kannada",
        "__v": 0,
        "isTeacherApp": true,
        "isPullModel": true,
        "isProcessed": true
      }
* */

public class CreateAudioFSMRequest {
    private AudioFileWithSpeechRates audioFiles;
    private String titleAudio;
    private String title;
    private String engTitle;
    private String description;
    private String id;
    private String type;
    private String language;
    private boolean isTeacherApp;
    private boolean isPullModel;

    public CreateAudioFSMRequest() {
        language = Constants.KANNADA_LANG;
    }

    public boolean isTeacherApp() {
        return isTeacherApp;
    }

    public String getTitleAudio() {
        return titleAudio;
    }

    public void setTitleAudio(String titleAudio) {
        this.titleAudio = titleAudio;
    }

    public void setTeacherApp(boolean teacherApp) {
        isTeacherApp = teacherApp;
    }

    public boolean isPullModel() {
        return isPullModel;
    }

    public void setPullModel(boolean pullModel) {
        isPullModel = pullModel;
    }

    public String getEngTitle() {
        return engTitle;
    }

    public void setEngTitle(String engTitle) {
        this.engTitle = engTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AudioFileWithSpeechRates getAudioFiles() {
        return audioFiles;
    }

    public void setAudioFiles(AudioFileWithSpeechRates audioFiles) {
        this.audioFiles = audioFiles;
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

    public boolean isValid(){
        return this.id != null && this.audioFiles != null && this.type != null && this.language != null;
    }


    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("audioFiles", audioFiles.toJSON());
        res.put("language", language);
        res.put("type", type);
        res.put("title", title);
        res.put("engTitle", engTitle);
        res.put("description", description);
        res.put("isTeacherApp", isTeacherApp);
        res.put("isPullModel", isPullModel);
        res.put("titleAudio", titleAudio);
        return res;
    }

    //*
    //    {
    //        "_id": "63700b90cc4dfe6a480f7f6b",
    //        "description": "",
    //        "language": "english",
    //        "title": "Water Harvesting Success",
    //        "url": "https://seedsblob.blob.core.windows.net/experience-titles/Story/6",
    //        "isPullModel": true,
    //        "isProcessed": true,
    //        "isTeacherApp": true,
    //        "type": "Story",
    //        "id": "6"
    //    }
    // *//

    public static CreateAudioFSMRequest fromJSON(JSONObject jsonObject){
        CreateAudioFSMRequest res = new CreateAudioFSMRequest();
        res.setId(jsonObject.getString("id"));
        if(jsonObject.has("audioFiles"))
            res.setAudioFiles(AudioFileWithSpeechRates.fromJSON(jsonObject.getJSONObject("audioFiles")));
        if(jsonObject.has("titleAudio") && !jsonObject.getString("titleAudio").isEmpty())
            res.setTitleAudio(jsonObject.getString("titleAudio"));
        res.setLanguage(jsonObject.getString("language"));
        res.setType(jsonObject.getString("type"));
        res.setTitle(jsonObject.getString("title"));
        if(jsonObject.has("engTitle")){
            res.setEngTitle(jsonObject.getString("engTitle"));
        }else{
            res.setEngTitle(jsonObject.getString("title"));
        }
        res.setDescription(jsonObject.getString("description"));
        res.setTeacherApp(jsonObject.getBoolean("isTeacherApp"));
        res.setPullModel(jsonObject.getBoolean("isPullModel"));
        return res;
    }

    public static CreateAudioFSMRequest getDummyAudioFSMRequest(){
        CreateAudioFSMRequest res = new CreateAudioFSMRequest();
        res.setId("dummy audio");
        res.setAudioFiles(new AudioFileWithSpeechRates(
                new ArrayList<>(Arrays.asList(
                        new AudioFile("1", 100, 1.0),
                        new AudioFile("2", 200, 1.5)
                ))));
        return res;
    }

    public void resetAudioFiles(){
        String basePath = "https://seedsblob.blob.core.windows.net/output-container/";
        setAudioFiles(
                AudioFileWithSpeechRates.getFor(basePath + this.id, "wav"));
    }

    public static List<CreateAudioFSMRequest> getAutomatedRequest(){
        String str = "Story Book - 1 - English\n" +
                "Hindi Song1 - 2 - Hindi\n" +
                "Hindi Song2 - 3 - Hindi\n" +
                "Panchatantra Friends - 4 - English\n" +
                "Saving Trees - 5 - English\n" +
                "Water Harvesting Success - 6 - English\n" +
                "Healthy Surroundings - 7 - English\n" +
                "Healthy Body and Healthy Mind - 8 - English\n" +
                "Dream English Traditional - 9 - English\n" +
                "How Are You - 10 - English\n" +
                "One Little Kitten - 11 - English\n" +
                "Rakshaneya Vidhanagalu - 12 - Kannada\n" +
                "Timma Kannada - 13 - Kannada\n" +
                "Ba Ba Maleraya - 14 - Kannada\n" +
                "Hattu Hattu Ipattu - 15 - Kannada\n" +
                "Aane banthondu aane - 16 - Kannada\n" +
                "Kokoko Kozhi Rhyme - 17 - Kannada\n" +
                "bayarida kaage story - 18 - Kannada\n" +
                "Buddhivantha nari Story - 19 - Kannada\n" +
                "Snehitaru - 20 - Kannada\n" +
                "Rhyme Ondu Kadina - 21 - Kannada\n" +
                "Kannada Rajyotsava speech 2022 - 27 - Kannada\n" +
                "Kittur Rani Chennamma life story - 28 - Kannada\n" +
                "He Shaarade - 36 - Kannada\n" +
                "Bendekayi Thondekayi - 37 - Kannada\n" +
                "Ondu Eradu Balale Haradu - 38 - Kannada\n" +
                "Karadi bettakke hogithu - 39 - Kannada\n" +
                "My Name is Madhavi - 40 - English\n" +
                "Twinkle Twinkle Little Star - 41 - English\n" +
                "Sahanu mattu chhotu - 42 - Kannada" ;
        List<String> lines = new ArrayList<>(Arrays.asList(str.split("\n")));
        List<CreateAudioFSMRequest> res = new ArrayList<>();
        for(String line : lines){
            //System.out.println(line);
            List<String> parts = new ArrayList<>(Arrays.asList(line.split("-")));
            String audioTitle = parts.get(0).trim();
            String audioID = parts.get(1).trim();
            String language = parts.get(2).trim();

            CreateAudioFSMRequest createAudioFSMRequest = new CreateAudioFSMRequest();
            createAudioFSMRequest.setId(audioTitle);
            createAudioFSMRequest.setLanguage(language);
            createAudioFSMRequest.setAudioFiles(AudioFileWithSpeechRates.getFor(audioID));
            res.add(createAudioFSMRequest);
        }
        return res;
    }
}
