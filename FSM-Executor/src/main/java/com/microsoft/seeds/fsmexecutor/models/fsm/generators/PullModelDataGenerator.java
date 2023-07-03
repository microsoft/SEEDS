package com.microsoft.seeds.fsmexecutor.models.fsm.generators;

import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.fsmexecutor.models.fsm.RawData;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelLeafNodeOption;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelNodeOption;
import com.microsoft.seeds.fsmexecutor.models.request.CreateAudioFSMRequest;
import com.microsoft.seeds.fsmexecutor.models.request.CreateQuizRequest;
import com.microsoft.seeds.fsmexecutor.models.utils.*;

import java.util.*;
import java.util.stream.Collectors;

public class PullModelDataGenerator {
    private static Map<String, String> stringInterpolationMap = new HashMap<>();
    private static String storyNodeId = Constants.STORY_FSM_TYPE;
    private static String rhymeNodeId = Constants.RHYMES_FSM_TYPE;
    private static String songNodeId = Constants.SONGS_FSM_TYPE;
    private static String quizNodeId = Constants.QUIZ_FSM_TYPE;
    private static List<PullModelLeafNodeOption> stories = new ArrayList<>();
    private static List<PullModelLeafNodeOption> rhymes = new ArrayList<>();
    private static List<PullModelLeafNodeOption> songs = new ArrayList<>();
    private static List<PullModelLeafNodeOption> quizzes = new ArrayList<>();

    public static PullModelData getPullModelData(String language,
                                                 List<ExpFSM> fsmList,
                                                 List<OnDemandFSMData> onDemandFSMDataList,
                                                 List<RawData> rawDataList){
        PullModelData res = new PullModelData("PullModel_" + language);
        stringInterpolationMap.put("lang", language.toLowerCase());
        resetAllLists();
        HashMap<String, RawData> rawDataHashMap = getRawDataMap(rawDataList);
        setStoryRhymesSongsList(fsmList, rawDataHashMap, language); // DIVIDE EXP DATA INTO CATEGORIES -> Songs, rhymes, stories
        setQuizList(onDemandFSMDataList, rawDataHashMap, language); // DIVIDE EXP DATA INTO CATEGORIES -> Quizzes

        res.setInitialMessage(getRootNodeAudioFiles()); // SET INITIAL WELCOME TO SEEDS MESSAGE
        res.addRootNode(); // CREATE ROOT NODE

        /* ADDING root/story
        *               |__ Story 1
        *               |__ Story 2
        *                   ...
        * */
        List<PullModelNodeOption> treeLegOptionsForStories = new ArrayList<>();
        PullModelNodeOption rootOptionForStories = new PullModelNodeOption(getStoryOptionAudioFile());
        treeLegOptionsForStories.add(rootOptionForStories);
        treeLegOptionsForStories.addAll(stories);
        PullModelUpdateResponse response = res.addNodeLeg(
                "root/"+ storyNodeId,
                treeLegOptionsForStories,
                Collections.singletonList(getStoryNodeAudioFiles()));
        System.out.println(response.getMessage());

        /* ADDING root/songs
         *               |__ Song 1
         *               |__ Song 2
         *                   ...
         * */
        List<PullModelNodeOption> treeLegOptionsForSongs = new ArrayList<>();
        PullModelNodeOption rootOptionForSongs = new PullModelNodeOption(getSongOptionAudioFile());
        treeLegOptionsForSongs.add(rootOptionForSongs);
        treeLegOptionsForSongs.addAll(songs);
        PullModelUpdateResponse response1 = res.addNodeLeg(
                "root/"+ songNodeId,
                treeLegOptionsForSongs,
                Collections.singletonList(getSongNodeAudioFiles()));
        System.out.println(response1.getMessage());

        /* ADDING root/rhymes
         *               |__ Rhyme 1
         *               |__ Rhyme 2
         *                   ...
         * */
        List<PullModelNodeOption> treeLegOptionsForRhymes = new ArrayList<>();
        PullModelNodeOption rootOptionForRhymes = new PullModelNodeOption(getRhymeOptionAudioFile());
        treeLegOptionsForRhymes.add(rootOptionForRhymes);
        treeLegOptionsForRhymes.addAll(rhymes);
        PullModelUpdateResponse response2 = res.addNodeLeg(
                "root/"+ rhymeNodeId,
                treeLegOptionsForRhymes,
                Collections.singletonList(getRhymeNodeAudioFiles()));
        System.out.println(response2.getMessage());

        /* ADDING root/quiz
         *               |__ Quiz 1
         *               |__ Quiz 2
         *                   ...
         * */
        List<PullModelNodeOption> treeLegOptionsForQuizzes = new ArrayList<>();
        PullModelNodeOption rootOptionForQuizzes = new PullModelNodeOption(getQuizOptionAudioFile());
        treeLegOptionsForQuizzes.add(rootOptionForQuizzes);
        treeLegOptionsForQuizzes.addAll(quizzes);
        PullModelUpdateResponse response3 = res.addNodeLeg(
                "root/"+ quizNodeId,
                treeLegOptionsForQuizzes,
                Collections.singletonList(getQuizNodeAudioFiles()));
        System.out.println(response3.getMessage());
        return res;
    }

    private static void resetAllLists(){
        stories = new ArrayList<>();
        rhymes = new ArrayList<>();
        songs = new ArrayList<>();
        quizzes = new ArrayList<>();
    }

    private static void setStoryRhymesSongsList(List<ExpFSM> fsmList, HashMap<String, RawData> rawDataMap, String language){
        fsmList.forEach(fsm -> {
            RawData rawData = rawDataMap.get(fsm.getId());
            if(rawData != null && rawData.getData().has("title")
                    && fsm.getLanguage().equalsIgnoreCase(language) ) {
                switch (fsm.getType().toLowerCase()) {
                    case Constants.STORY_FSM_TYPE:
                        CreateAudioFSMRequest storyReq = CreateAudioFSMRequest.fromJSON(rawData.getData());
                        if(storyReq.getTitleAudio() != null) {
                            stories.add(new PullModelLeafNodeOption(
                                    fsm.getId(),
                                    Constants.FSM_TYPE_STATIC,
                                    rawData.getData().getString("title"),
                                    AudioFileWithSpeechRates.getFor(storyReq.getTitleAudio()),
                                    rawData.timeStamp));
                        }
                        break;
                    case Constants.RHYMES_FSM_TYPE:
                        CreateAudioFSMRequest rhymesReq = CreateAudioFSMRequest.fromJSON(rawData.getData());
                        if(rhymesReq.getTitleAudio() != null) {
                            rhymes.add(new PullModelLeafNodeOption(
                                    fsm.getId(),
                                    Constants.FSM_TYPE_STATIC,
                                    rawData.getData().getString("title"),
                                    AudioFileWithSpeechRates.getFor(rhymesReq.getTitleAudio()),
                                    rawData.timeStamp));
                        }
                        break;
                    case Constants.SONGS_FSM_TYPE:
                        CreateAudioFSMRequest songsReq = CreateAudioFSMRequest.fromJSON(rawData.getData());
                        if(songsReq.getTitleAudio() != null) {
                            songs.add(new PullModelLeafNodeOption(
                                    fsm.getId(),
                                    Constants.FSM_TYPE_STATIC,
                                    rawData.getData().getString("title"),
                                    AudioFileWithSpeechRates.getFor(songsReq.getTitleAudio()),
                                    rawData.timeStamp));
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        stories = getSorted(stories);
        songs = getSorted(songs);
        rhymes = getSorted(rhymes);
//
//        System.out.println("LANGUAGE: " + language);
//        System.out.println("STORIES");
//        stories.forEach(story -> System.out.println(story.getGraphString()));
//        System.out.println("RHYMES");
//        rhymes.forEach(rhyme -> System.out.println(rhyme.getGraphString()));
//        System.out.println("SONGS");
//        songs.forEach(song -> System.out.println(song.getGraphString()));
    }

    private static void setQuizList(List<OnDemandFSMData> onDemandFSMDataList, HashMap<String, RawData> rawDataMap, String language){
        onDemandFSMDataList.forEach(onDemandFSMData -> {
            CreateQuizRequest createQuizRequest = null;
            RawData rawData = rawDataMap.get(onDemandFSMData.getId());
            if(rawData != null && rawData.getData().has("title") && isQuiz(onDemandFSMData)) {
                createQuizRequest = CreateQuizRequest.fromJSON(onDemandFSMData.data);
                if(createQuizRequest.getTitleAudio() != null) {
                    quizzes.add(new PullModelLeafNodeOption(createQuizRequest.getId(),
                            Constants.FSM_TYPE_ON_DEMAND,
                            rawData.getData().getString("title"),
                            AudioFileWithSpeechRates.getFor(createQuizRequest.getTitleAudio()),
                            rawData.timeStamp));
                }
            }
        });
        quizzes = getSorted(quizzes);

//        System.out.println("QUIZZES");
//        quizzes.forEach(quiz -> System.out.println(quiz.getGraphString()));
    }

    private static HashMap<String, RawData> getRawDataMap(List<RawData> rawDataList){
        HashMap<String, RawData> res = new HashMap<>();
        rawDataList.forEach(rawData -> {
            res.put(rawData.getId(), rawData);
        });
        return res;
    }

    private static List<PullModelLeafNodeOption> getSorted(List<PullModelLeafNodeOption> options){
        return options.stream()
                .sorted((a, b) -> {
                    if (a.getCreationTimestamp() < b.getCreationTimestamp()) return 1;
                    if (a.getCreationTimestamp() > b.getCreationTimestamp()) return -1;
                    return 0;
                })
                .collect(Collectors.toList());
    }

    private static List<AudioFileWithSpeechRates> getRootNodeAudioFiles(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.WELCOME_TO_SEEDS, stringInterpolationMap);
        return new ArrayList<>(Collections.singletonList(
                AudioFileWithSpeechRates.getFor(path)));
    }

    private static List<AudioFileWithSpeechRates> getStoryNodeAudioFiles(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_NODE_STORIES, stringInterpolationMap);
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor(path)));
    }

    private static List<AudioFileWithSpeechRates> getSongNodeAudioFiles(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_NODE_SONGS, stringInterpolationMap);
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor(path)));
    }

    private static List<AudioFileWithSpeechRates> getRhymeNodeAudioFiles(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_NODE_RHYMES, stringInterpolationMap);
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor(path)));
    }

    private static List<AudioFileWithSpeechRates> getQuizNodeAudioFiles(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_NODE_QUIZ, stringInterpolationMap);
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor(path)));
    }

    private static AudioFileWithSpeechRates getStoryOptionAudioFile(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_DIALOG_FOR_STORIES, stringInterpolationMap);
        return AudioFileWithSpeechRates.getFor(path);
    }

    private static AudioFileWithSpeechRates getSongOptionAudioFile(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_DIALOG_FOR_SONGS, stringInterpolationMap);
        return AudioFileWithSpeechRates.getFor(path);
    }

    private static AudioFileWithSpeechRates getRhymeOptionAudioFile(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_DIALOG_FOR_RHYMES, stringInterpolationMap);
        return AudioFileWithSpeechRates.getFor(path);
    }

    private static AudioFileWithSpeechRates getQuizOptionAudioFile(){
        String path = StringInterpolator.substituteForMap(PullModelMenuAudioPaths.EXP_DIALOG_TO_PLAY_QUIZ, stringInterpolationMap);
        return AudioFileWithSpeechRates.getFor(path);
    }

    private static boolean isStorySongRhyme(ExpFSM fsm){
        return fsm.getType().equalsIgnoreCase(Constants.STORY_FSM_TYPE) ||
                fsm.getType().equalsIgnoreCase(Constants.SONGS_FSM_TYPE) ||
                fsm.getType().equalsIgnoreCase(Constants.RHYMES_FSM_TYPE);
    }

    private static boolean isQuiz(OnDemandFSMData onDemandFSMData){
        return onDemandFSMData.getType().equalsIgnoreCase(Constants.QUIZ_FSM_TYPE);
    }
}
