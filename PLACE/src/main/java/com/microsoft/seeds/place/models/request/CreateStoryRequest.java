package com.microsoft.seeds.place.models.request;

import com.microsoft.seeds.place.models.utils.StoryLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateStoryRequest {
    public String name;
    public String text;
    private static final double SECONDS_PER_WORD = 0.4;

    public CreateStoryRequest() {
    }

    public String getName() {
        return name;
    }

    public CreateStoryRequest setName(String name) {
        this.name = name;
        return this;
    }

    public String getText() {
        return text;
    }

    public CreateStoryRequest setText(String text) {
        this.text = text;
        return this;
    }

    public List<StoryLine> getStoryLines(){
        List<String> lines =  new ArrayList<>(Arrays.asList(text.split("\\.")));
        List<StoryLine> storyLines = new ArrayList<>();
        for (String line : lines) {
            int numWords = line.split(" ").length;
            storyLines.add(new StoryLine(line.trim() + ".", numWords * SECONDS_PER_WORD));
        }
        return storyLines;
    }

    public static CreateStoryRequest getDummyStoryRequest(){
        CreateStoryRequest createStoryRequest = new CreateStoryRequest();
        createStoryRequest.setText("A black hole is a region of spacetime where gravity is so strong that nothing – " +
                "no particles or even electromagnetic radiation such as light – can escape from it. The theory of general relativity " +
                "predicts that a sufficiently compact mass can deform spacetime to form a black hole. The boundary of no escape is called " +
                "the event horizon. Although it has a great effect on the fate and circumstances of an object crossing it, it has no locally " +
                "detectable features according to general relativity. In many ways, a black hole acts like an ideal black body, as it reflects no light. " +
                "Moreover, quantum field theory in curved spacetime predicts that event horizons emit Hawking radiation, with the same spectrum as a " +
                "black body of a temperature inversely proportional to its mass. This temperature is of the order of billionths of a kelvin for stellar black holes, " +
                "making it essentially impossible to observe directly.");
        createStoryRequest.setName("Black Holes");
        return createStoryRequest;
    }

    public static List<CreateStoryRequest> getListOfDummyRequests(){
        List<CreateStoryRequest> createStoryRequestList = new ArrayList<>();
        createStoryRequestList.add(getDummyStoryRequest());
        createStoryRequestList.add(new CreateStoryRequest()
                        .setName("Design Process")
                        .setText("What is design process?" +
                        "If you’ve tried to learn about Design Process, it might seem mysterious or complex, but in truth it should be neither." +
                        "Design Process is often described as a problem solving process, but this is one reason it might seem confusing. " +
                        "If you have seen a list of steps for the design process, the first step was probably “define the problem”." +
                        "However, many designers don’t have a problem to solve, so the very first step they see isn’t useful." +
                        "Not all designs are focused purely around problems. " +
                        "Instead of problems to solve, often designers have a goal to meet (like selling a product) or are asked to create a specific " +
                        "deliverable (like a logo or web design).")
                        );
        createStoryRequestList.add(new CreateStoryRequest()
                        .setName("The Hare and the tortoise")
                        .setText("A Hare was making fun of the Tortoise one day for being so slow." +
                                "\"Do you ever get anywhere?\" he asked with a mocking laugh." +
                                "\"Yes,\" replied the Tortoise, \"and I get there sooner than you think. I'll run you a race and prove it.\"" +
                                "The Hare was much amused at the idea of running a race with the Tortoise, but for the fun of the thing he agreed. " +
                                "So the Fox, who had consented to act as judge, marked the distance and started the runners off." +
                                "The Hare was soon far out of sight, and to make the Tortoise feel very deeply how ridiculous it was for him to try a race with a Hare, " +
                                "he lay down beside the course to take a nap until the Tortoise should catch up. " +
                                "The Tortoise meanwhile kept going slowly but steadily, and, after a time, passed the place where the Hare was sleeping. " +
                                "But the Hare slept on very peacefully; and when at last he did wake up, the Tortoise was near the goal. " +
                                "The Hare now ran his swiftest, but he could not overtake the Tortoise in time."));
        return createStoryRequestList;
    }
}
