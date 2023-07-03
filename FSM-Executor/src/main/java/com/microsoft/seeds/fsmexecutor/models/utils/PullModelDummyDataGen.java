package com.microsoft.seeds.fsmexecutor.models.utils;

import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.fsmexecutor.models.fsm.pullmodel.PullModelNodeOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PullModelDummyDataGen {
    public static PullModelData getDummy(){
        PullModelData res = new PullModelData("DUMMY");
        String story_node_id = "story";
        String rhyme_node_id = "rhyme";
        res.setInitialMessage(getDummyRootNodeAudioFiles());
        res.addRootNode();

        /* ADDING LEG root/english/story
                                    |__ Story 1
                                    |__ Story 2
         */
        List<PullModelNodeOption> legOptionsForEnglish = new ArrayList<>(
                Collections.singletonList(
                        new PullModelNodeOption(AudioFileWithSpeechRates.getFor("dummyContainer/dummyEngOption"))));
        legOptionsForEnglish.addAll(getPullModelNodeLegOptionForStories());
        List<List<AudioFileWithSpeechRates>> audioFilesForEngAndStoryNode = new ArrayList<>();
        audioFilesForEngAndStoryNode.add(getDummyEngNodeAudioFiles());
        audioFilesForEngAndStoryNode.add(getDummyEngStoryNodeAudioFiles());
        PullModelUpdateResponse response = res.addNodeLeg("root/english/"+ story_node_id, legOptionsForEnglish, audioFilesForEngAndStoryNode);
        System.out.println(response.getMessage());

        /* ADDING LEG root/kannada/rhyme
                                    |__ Rhyme 1
                                    |__ Rhyme 2
         */
        List<PullModelNodeOption> legOptionsForKannada = new ArrayList<>(
                Collections.singletonList(
                        new PullModelNodeOption(AudioFileWithSpeechRates.getFor("dummyContainer/dummyKnOption"))));
        legOptionsForKannada.addAll(getPullModelNodeLegOptionForRhymes());
        List<List<AudioFileWithSpeechRates>> audioFilesForKnRhymesNode = new ArrayList<>();
        audioFilesForKnRhymesNode.add(getDummyKannadaNodeAudioFiles());
        audioFilesForKnRhymesNode.add(getDummyKnRhymesNodeAudioFiles());
        PullModelUpdateResponse response1 = res.addNodeLeg("root/kannada/"+ rhyme_node_id, legOptionsForKannada, audioFilesForKnRhymesNode);
        System.out.println(response1.getMessage());

        /* ADDING LEG root/english/rhyme
                                    |__ Rhyme 1
                                    |__ Rhyme 2
         */
        List<List<AudioFileWithSpeechRates>> audioFilesForEngRhymesNode = new ArrayList<>();
        audioFilesForEngRhymesNode.add(getDummyEngRhymesNodeAudioFiles());
        PullModelUpdateResponse response2 = res.addNodeLeg("root/english/"+ rhyme_node_id, getPullModelNodeLegOptionForRhymes(), audioFilesForEngRhymesNode);
        System.out.println(response2.getMessage());

        /* ADDING LEAF OPTION root/english/story
                                    |__ Story EXTRA
                                    |__ Story EXTRA
         */
        PullModelUpdateResponse response3 = res.addLeafOptions(story_node_id, "english", getExtraLeafStoriesOptions());
        System.out.println(response3.getMessage());

        return res;
    }

    private static List<AudioFileWithSpeechRates> getDummyRootNodeAudioFiles() {
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyRootNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyLangNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyLangNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyEngNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyEngNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyKannadaNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyKannadaNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyEngStoryNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyEngStoryNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyEngRhymesNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyEngRhymesNode")));
    }

    private static List<AudioFileWithSpeechRates> getDummyKnRhymesNodeAudioFiles(){
        return new ArrayList<>(Collections.singletonList(AudioFileWithSpeechRates.getFor("dummyContainer/dummyKnRhymesNode")));
    }

    private static List<PullModelNodeOption> getPullModelNodeLegOptionForStories(){
        PullModelNodeOption englishOptionForStories = new PullModelNodeOption(AudioFileWithSpeechRates.getFor("dummyContainer/dummyStoryOption"));
        List<PullModelNodeOption> storyOptions = new ArrayList<>(Arrays.asList(
                PullModelNodeOption.getDummy("Story 1", true),
                PullModelNodeOption.getDummy("Story 2", true)));
        List<PullModelNodeOption> res = new ArrayList<>(Arrays.asList(englishOptionForStories));
        res.addAll(storyOptions);
        return res;
    }

    private static List<PullModelNodeOption> getExtraLeafStoriesOptions(){
        List<PullModelNodeOption> storyOptions = new ArrayList<>(Arrays.asList(
                PullModelNodeOption.getDummy("Story EXTRA", true),
                PullModelNodeOption.getDummy("Story EXTRA", true)));
        return storyOptions;
    }

    private static List<PullModelNodeOption> getPullModelNodeLegOptionForRhymes(){
        PullModelNodeOption OptionForRhymes = new PullModelNodeOption(AudioFileWithSpeechRates.getFor("dummyContainer/dummyRhymesOption"));
        List<PullModelNodeOption> rhymesOptions = new ArrayList<>(Arrays.asList(
                PullModelNodeOption.getDummy("Rhyme 1", true),
                PullModelNodeOption.getDummy("Rhyme 2", true)));
        List<PullModelNodeOption> res = new ArrayList<>(Arrays.asList(OptionForRhymes));
        res.addAll(rhymesOptions);
        return res;
    }
}
