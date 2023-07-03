package com.microsoft.seeds.place.models.utils;

import java.util.HashMap;
import java.util.Map;

public class QuizStaticDialogs {
    private static final String KANNADA_CONGRATULATIONS_FOR_CORRECT_ANSWER = "ಅಭಿನಂದನೆಗಳು! ನೀವು ಸರಿಯಾದ ಉತ್ತರವನ್ನು ಆರಿಸಿದ್ದೀರಿ ";
    private static final String KANNADA_WRONG_ANSWER = "ಅರೆರೆ! ನೀವು ತಪ್ಪು ಉತ್ತರವನ್ನು ಆರಿಸಿದ್ದೀರಿ,";
    private static final String KANNADA_YOUR_SCORE_IS = "ನಿಮ್ಮ ಸ್ಕೋರ್ ಆಗಿದೆ ";
    private static final String KANNADA_FINAL_SCORE = "ನಿಮ್ಮ ಅಂತಿಮ ಸ್ಕೋರ್ ";
    private static final String KANNADA_THANKS_FOR_PARTICIPATING = "ಭಾಗವಹಿಸಿದ್ದಕ್ಕಾಗಿ ಧನ್ಯವಾದಗಳು!";
    private static final String KANNADA_ENGLISH_MOVING_TO_NEXT_QUESTION = "ಮುಂದಿನ ಪ್ರಶ್ನೆಗೆ ಹೋಗೋಣ!";

    private static final String ENGLISH_CONGRATULATIONS_FOR_CORRECT_ANSWER = "Congratulations! You have chosen the correct answer,";
    private static final String ENGLISH_WRONG_ANSWER = "Oh no! You chose the wrong answer,";
    private static final String ENGLISH_YOUR_SCORE_IS = "your score is,";
    private static final String ENGLISH_FINAL_SCORE = "Your final score is,";
    private static final String ENGLISH_THANKS_FOR_PARTICIPATING = "Thanks for participating!";

    private static final String ENGLISH_MOVING_TO_NEXT_QUESTION = "Let's move on to next question.";

    private static final String CONGRATULATIONS_FOR_CORRECT_ANSWER = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/correctAnswer/Congratulations!%20You%20have%20chosen%20the%20correct%20answer,";
    private static final String WRONG_ANSWER = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/wrongAnswer/Oh%20no!%20You%20chose%20the%20wrong%20answer,";
    private static final String YOUR_SCORE_IS = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/yourScore/your%20Score%20is,";
    private static final String FINAL_SCORE = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/yourFinalScore/Your%20final%20score%20is,";
    private static final String THANKS_FOR_PARTICIPATING = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/thanksForParticipating/Thanks%20for%20participating!";
    private static final String MOVING_TO_NEXT_QUESTION = "https://seedsblob.blob.core.windows.net/pull-model-menus/experienceSpecificDialog/${lang}/quiz/movingToNextQuestion/Let's%20move%20on%20to%20next%20question";

    public String congratulations;
    public String wrongAnswer;
    public String yourScoreIs;
    public String finalScoreIs;
    public String thanksForParticipating;

    public String movingOnToNextQuestion;

    public QuizStaticDialogs(String language){
        Map<String, String> languageReplacementMap  = new HashMap<String, String>() {{
            put("lang", language.toLowerCase());
        }};
        this.congratulations = StringInterpolator.substituteForMap(CONGRATULATIONS_FOR_CORRECT_ANSWER, languageReplacementMap);
        this.wrongAnswer = StringInterpolator.substituteForMap(WRONG_ANSWER, languageReplacementMap);
        this.yourScoreIs = StringInterpolator.substituteForMap(YOUR_SCORE_IS, languageReplacementMap);
        this.finalScoreIs = StringInterpolator.substituteForMap(FINAL_SCORE, languageReplacementMap);
        this.thanksForParticipating = StringInterpolator.substituteForMap(THANKS_FOR_PARTICIPATING, languageReplacementMap);
        this.movingOnToNextQuestion = StringInterpolator.substituteForMap(MOVING_TO_NEXT_QUESTION, languageReplacementMap);
    }
}
