package com.microsoft.seeds.place.models.request;

import com.microsoft.seeds.place.models.fsm.RawData;
import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.place.models.utils.Constants;
import com.microsoft.seeds.place.models.utils.JSONParse;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreateQuizRequest {
    public String id;
    public String title;
    public String titleAudio;
    public List<Double> speechRates;
    public List<String> questions;
    public List<List<String>> options;
    public List<Integer> correctAnswers;
    public int positiveMark;
    public int negativeMark;

    public String language;

    public static String DUMMY_QUIZ_NAME = "General Trivia";

    public CreateQuizRequest() {
        language = Constants.ENGLISH_LANG;
        speechRates = Constants.AUDIO_FILE_SPEECH_RATES;
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleAudio() {
        return titleAudio;
    }

    public void setTitleAudio(String titleAudio) {
        this.titleAudio = titleAudio;
    }

    public List<Double> getSpeechRates() {
        return speechRates;
    }

    public void setSpeechRates(List<Double> speechRates) {
        this.speechRates = speechRates;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language.toLowerCase();
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public List<List<String>> getOptions() {
        return options;
    }

    public void setOptions(List<List<String>> options) {
        this.options = options;
    }

    public List<Integer> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<Integer> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getPositiveMark() {
        return positiveMark;
    }

    public void setPositiveMark(int positiveMark) {
        this.positiveMark = positiveMark;
    }

    public int getNegativeMark() {
        return negativeMark;
    }

    public void setNegativeMark(int negativeMark) {
        this.negativeMark = negativeMark;
    }

    public boolean isValid(){
        return questions.size() == options.size() && options.size() == correctAnswers.size();
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("id", id);
        res.put("title", title);
        res.put("questions", questions);
        res.put("options", options);
        res.put("correctAnswers", correctAnswers);
        res.put("positiveMark", positiveMark);
        res.put("negativeMark", negativeMark);
        res.put("language", language);
        res.put("speechRates", JSONParse.getJSONArrayFromDoubleList(speechRates));
        if(titleAudio != null){
            res.put("titleAudio", titleAudio);
        }
        return res;
    }

    public static CreateQuizRequest fromJSON(JSONObject jsonObject){
        CreateQuizRequest res = new CreateQuizRequest();
        try {
            res.setId(jsonObject.getString("id"));
            res.setTitle(jsonObject.getString("title"));
            res.setQuestions(JSONParse.getStringList(jsonObject.getJSONArray("questions")));
            res.setOptions(JSONParse.get2DStringList(jsonObject.getJSONArray("options")));
            res.setCorrectAnswers(JSONParse.getIntList(jsonObject.getJSONArray("correctAnswers")));
            res.setPositiveMark(jsonObject.getInt("positiveMark"));
            res.setNegativeMark(jsonObject.getInt("negativeMark"));
            res.setLanguage(jsonObject.getString("language"));
            if(jsonObject.has("speechRates")) {
                res.setSpeechRates(JSONParse.getDoubleList(jsonObject.getJSONArray("speechRates")));
            }
            if(jsonObject.has("titleAudio")){
                res.setTitleAudio(jsonObject.getString("titleAudio"));
            }
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        return res;
    }

    public static CreateQuizRequest getFromDummyJSON(){
        String json = "{\"id\": \"8a7c7e98-a769-11ed-afa1-0242ac120002\",\"type\":\"quiz\",\"positiveMark\":1,\"negativeMark\":0,\"title\":\"Mahabharata quiz 3\",\"questions\":[\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/question_1\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/question_2\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/question_3\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/question_4\"],\"options\":[[\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_1/1\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_1/2\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_1/3\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_1/4\"],[\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_2/1\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_2/2\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_2/3\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_2/4\"],[\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_3/1\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_3/2\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_3/3\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_3/4\"],[\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_4/1\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_4/2\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_4/3\",\"https://seedsblob.blob.core.windows.net/output-container/Quiz/Mahabharata%20quiz%203/option_4/4\"]],\"language\":\"Kannada\",\"correctAnswers\":[0,0,0,0],\"speechRates\":[\"0.5\",\"0.75\",\"1.0\",\"1.5\",\"2.0\"]}";
        JSONObject jsonObject = new JSONObject(json);
        return CreateQuizRequest.fromJSON(jsonObject);
    }

    public List<AudioFileWithSpeechRates> getQuestionsAudioFileWithSpeechRates(){
        List<AudioFileWithSpeechRates> audios = new ArrayList<>();
        for(String path: questions) {
            audios.add(AudioFileWithSpeechRates.getFor(path, this.speechRates));
        }
        return audios;
    }

    public List<AudioFileWithSpeechRates> getOptionAudioFileWithSpeechRatesFor(int index){
        List<AudioFileWithSpeechRates> audios = new ArrayList<>();
        for(String option: this.options.get(index)){
            audios.add(AudioFileWithSpeechRates.getFor(option, this.speechRates));
        }
        return audios;
    }
    public List<AudioFileWithSpeechRates> getAudioFileWithSpeechRatesList(List<String> paths){
        List<AudioFileWithSpeechRates> audios = new ArrayList<>();
        for(String path: paths){
            audios.add(AudioFileWithSpeechRates.getFor(path, this.speechRates));
        }
        return audios;
    }

    public static CreateQuizRequest getDummyQuizRequest(){
        CreateQuizRequest quizRequest = new CreateQuizRequest();
        List<String> questions = new ArrayList<>(
                Arrays.asList(
                        "How many legs does a spider have?",
                        "What is something you hit with a hammer?",
                        "What's the name of a place you go to see lots of animals?",
                        "Whose nose grew longer every time he lied?",
                        "If you freeze water, what do you get?"));
        List<List<String>> options = new ArrayList<>(Arrays.asList(
                Arrays.asList("two", "four", "six", "eight"),
                Arrays.asList("a nail", "a fruit", "a machine", "a bottle"),
                Arrays.asList("aquarium", "cinema", "zoo", "white house"),
                Arrays.asList("harry potter", "Pinocchio", "Gandalf", "Peter Pan"),
                Arrays.asList("Fire", "cold drink", "ice", "juice")));
        List<Integer> answers = new ArrayList<>(Arrays.asList(3, 0, 2, 1, 2));
        quizRequest.setTitle(DUMMY_QUIZ_NAME);
        quizRequest.setQuestions(questions);
        quizRequest.setOptions(options);
        quizRequest.setCorrectAnswers(answers);
        quizRequest.setNegativeMark(0);
        quizRequest.setPositiveMark(1);
        return quizRequest;
    }

    public static List<CreateQuizRequest> getListOfDummyRequests(){
        List<CreateQuizRequest> createQuizRequestList = new ArrayList<>();
        CreateQuizRequest quizRequest1 = new CreateQuizRequest();
        List<String> questions1 = new ArrayList<>(
                Arrays.asList(
                    "How many teeth does an adult human have?",
                    "What is the largest known living land animal?",
                    "What are animals that eat both meat and plants called?",
                    "What is the largest internal organ in the human body?",
                    "What is the world's largest ocean?"));
        List<List<String>> options1 = new ArrayList<>(Arrays.asList(
                Arrays.asList("two", "twenty", "thirty two", "fifty five"),
                Arrays.asList("elephant", "african elephant", "cow", "shark"),
                Arrays.asList("carnivores", "omnivores", "Herbivores", "Scavengers"),
                Arrays.asList("liver", "stomach", "Pancreas", "Lungs"),
                Arrays.asList("Atlantic Ocean", "Indian Ocean", "Pacific Ocean", "Arctic Ocean")));
        List<Integer> answers1 = new ArrayList<>(Arrays.asList(2, 1, 1, 0, 2));
        quizRequest1.setTitle("Science Trivia");
        quizRequest1.setQuestions(questions1);
        quizRequest1.setOptions(options1);
        quizRequest1.setCorrectAnswers(answers1);
        quizRequest1.setNegativeMark(0);
        quizRequest1.setPositiveMark(1);

        CreateQuizRequest quizRequest2 = new CreateQuizRequest();
        List<String> questions2 = new ArrayList<>(
                Arrays.asList(
                "Where did the Olympic games originate?",
                "Who was the first Black president of the United States?",
                "Which was the first country to use paper money?",
                "Who wrote a famous diary while hiding from Nazis in Amsterdam?",
                "In which year did the Titanic sink?"));
        List<List<String>> options2 = new ArrayList<>(Arrays.asList(
                Arrays.asList("Australia", "Greece", "India", "Pakistan"),
                Arrays.asList("Joe Biden", "Bill Clinton", "Donald Trump", "Barack Obama"),
                Arrays.asList("Australia", "China", "India", "Pakistan"),
                Arrays.asList("Anne Frank", "William Shakespeare", "John Green", "Dan Brown"),
                Arrays.asList("2022", "1857", "1984", "1912")));
        List<Integer> answers2 = new ArrayList<>(Arrays.asList(1, 3, 1, 0, 3));
        quizRequest2.setTitle("History Trivia");
        quizRequest2.setQuestions(questions2);
        quizRequest2.setOptions(options2);
        quizRequest2.setCorrectAnswers(answers2);
        quizRequest2.setNegativeMark(0);
        quizRequest2.setPositiveMark(1);

        createQuizRequestList.add(getDummyQuizRequest());
        createQuizRequestList.add(quizRequest1);
        createQuizRequestList.add(quizRequest2);
        return createQuizRequestList;
    }

    public RawData getRawData(){
        JSONObject res = this.toJSON();
        res.remove("speechRates");
        return new RawData(this.id, Constants.QUIZ_FSM_TYPE, res, false);
    }

}
