package com.microsoft.seeds.place.models.fsm.pullmodel;

import com.microsoft.seeds.place.models.utils.AudioFileWithSpeechRates;
import com.microsoft.seeds.place.models.utils.PullModelUpdateResponse;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.generate;

public class PullModelData {
    private static final String NODE_KEY_SEP = "__";
    private static final String ROOT_NODE_PARENT_ID = "NIL";
    private static final String ROOT_NODE_ID = "root";
    private static final int NUM_SPACES_IN_VISUALIZATION = 4;
    private String id;
    private HashMap<String, PullModelNode> nodes;

    private List<AudioFileWithSpeechRates> initialMessage;

    public PullModelData(String id) {
        this.id = id;
        this.nodes = new HashMap<>();
        this.initialMessage = new ArrayList<>();
    }

    public HashMap<String, PullModelNode> getNodes() {
        return nodes;
    }

    public String getId() {
        return id;
    }

    public List<AudioFileWithSpeechRates> getInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(List<AudioFileWithSpeechRates> initialMessage) {
        this.initialMessage = initialMessage;
    }

    public String getRootNodeKey(){
        return getKey(ROOT_NODE_ID, ROOT_NODE_PARENT_ID);
    }

    public void addRootNode(){
        this.nodes.put(getRootNodeKey(), new PullModelNode(ROOT_NODE_ID, ROOT_NODE_PARENT_ID, new ArrayList<>(), new ArrayList<>()));
    }
    private void addNode(PullModelNode node){
        this.nodes.put(getKey(node.getId(), node.getParentId()), node);
    }

    // ADD THE NODE ALL THE WAY TO THE LEAF
    /*
    List of options should be ordered such that:
        [(1, 2) , {3, 4, 5}]
            { nodes } => LEAF NODES
            ( nodes ) => intermediary node options i.e., each option belongs to a new node here
                         Option Ids here will be ignored and options will have ids equal to the pullModelPath
    Get all the nodes in the pullModelPath == root/english/story/class 1 -> class 1 stories options
    From the point where there are no existing nodes, check if you have one option for each of the following
        parent node
        new intermediary node(s)
        leaf node
    Add all the extra options to the last leaf node
     */
    public PullModelUpdateResponse addNodeLeg(String pullModelPath, List<PullModelNodeOption> options,
                                              List<List<AudioFileWithSpeechRates>> nodesAudioFiles){
        String[] pullModelPathParts = pullModelPath.split("/");
        if(!pullModelPathParts[0].equalsIgnoreCase(ROOT_NODE_ID)){
            return new PullModelUpdateResponse("PULL MODEL PATH SHOULD START WITH ROOT NODE ID: " + ROOT_NODE_ID, 400);
        }
        int leafOptionStartIndex = getStartIndexOfLeafOptions(options);
        if(leafOptionStartIndex == 0){
            return new PullModelUpdateResponse("ALL PROVIDED OPTIONS ARE LEAF OPTIONS, USE ADD LEAF OPTION FUNCTION", 400);
        }
        if(leafOptionStartIndex == options.size()){
            return new PullModelUpdateResponse("NO LEAF OPTION PROVIDED", 400);
        }

        PullModelNode curr = this.nodes.get(getRootNodeKey());
        int pullModelPathIndex;
        for(pullModelPathIndex = 1; pullModelPathIndex<pullModelPathParts.length; ++pullModelPathIndex){
            String optionId = pullModelPathParts[pullModelPathIndex];
            boolean flg = false;
            for (PullModelNodeOption option: curr.getOptions()) {
                if(option.getId().equalsIgnoreCase(optionId)){
                    curr = this.nodes.get(getKey(optionId, curr.getId()));
                    flg = true;
                    break;
                }
            }
            if(!flg){ // GOT THE PARENT NODE OF NEW NODE
                break;
            }
        }
        int numNewNodes = pullModelPathParts.length - pullModelPathIndex;
        if(numNewNodes != leafOptionStartIndex){
            return new PullModelUpdateResponse("NUM NEW NODES IN PULL MODEL PATH DOES NOT EQUAL NUM NON LEAF OPTIONS PROVIDED", 400);
        }
        if(numNewNodes != nodesAudioFiles.size()){
            return new PullModelUpdateResponse("NUM NEW NODES IN PULL MODEL PATH DOES NOT EQUAL NUM OF NODE AUDIOFILES PROVIDED", 400);
        }

        // ADD OPTION TO PARENT NODE AND THEN CREATE NEW NODE
        for(int i = 0; i < leafOptionStartIndex; ++i){
            PullModelNodeOption optionToAdd = options.get(i);
            String optionToAddId = pullModelPathParts[pullModelPathIndex++];
            optionToAdd.setId(optionToAddId);
            curr.addOption(optionToAdd);
            curr = new PullModelNode(optionToAddId, curr.getId(), new ArrayList<>(), nodesAudioFiles.get(i));
            addNode(curr);
        }

        //ADD ALL LEAF OPTIONS
        curr.setLeaf(true); // SET LAST NODE AS LEAF NODE
        for(int i = leafOptionStartIndex; i < options.size(); ++i){
            curr.addOption(options.get(i));
        }
        return new PullModelUpdateResponse("ADDED SUCCESSFULLY", 200);
    }

    public PullModelUpdateResponse addLeafOptions(String nodeId, String parentNodeId, List<PullModelNodeOption> options){
        if(!this.nodes.containsKey(getKey(nodeId, parentNodeId))){
            return new PullModelUpdateResponse("NO NODE WITH ID: " + nodeId + " AND PARENT NODE ID: " + parentNodeId, 400);
        }
        PullModelNode node = this.nodes.get(getKey(nodeId, parentNodeId));
        for(PullModelNodeOption option: options){
            if(!PullModelNodeOption.isLeafNodeOption(option.toJSON())) {
                return new PullModelUpdateResponse("ALL OPTIONS ARE NOT LEAF OPTIONS", 400);
            }
        }
        for(PullModelNodeOption option: options){
            if(PullModelNodeOption.isLeafNodeOption(option.toJSON())) {
                node.addOption(option);
            }
        }
        return new PullModelUpdateResponse("ADDED SUCCESSFULLY", 200);
    }

    public void removeNode(String nodeId, String parentId){
        String currNodeKey = getKey(nodeId, parentId);
        PullModelNode currNode = nodes.get(currNodeKey);
        nodes.remove(currNodeKey);

        //REMOVE ALL CHILD NODES OF THIS NODE
        currNode.getOptions().forEach(option -> {
            String optionNodeKey = getKey(option.getId(), currNode.getId());
            nodes.remove(optionNodeKey);
        });
    }

    public static String getKey(String nodeId, String parentId){
        return parentId + NODE_KEY_SEP + nodeId;
    }

    public PullModelNode getNode(String key){
        return this.nodes.get(key);
    }

    public PullModelNode getRootNode(){
        return getNode(getRootNodeKey());
    }

    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("_id", id);
        res.put("initialMessage",
                initialMessage.stream()
                .map(AudioFileWithSpeechRates::toJSON)
                .collect(Collectors.toList()));
        nodes.forEach((key, node)->{
            res.put(key, node.toJSON());
        });
        return res;
    }

    public static Optional<PullModelData> fromJSON(JSONObject jsonObject) {
        try {
            PullModelData res = new PullModelData(jsonObject.getString("_id"));
            res.setInitialMessage(
                    jsonObject.getJSONArray("initialMessage")
                    .toList()
                    .stream()
                    .map(audioFile -> AudioFileWithSpeechRates.fromJSON(new JSONObject((HashMap) audioFile)))
                    .collect(Collectors.toList()));
            jsonObject.keySet().forEach(key -> {
                if (key.contains(NODE_KEY_SEP)) { // the object of this key is a node
                    JSONObject nodeObj = jsonObject.getJSONObject(key);
                    res.addNode(PullModelNode.fromJSON(nodeObj));
                }
            });
            if (isValid(res))
                return Optional.of(res);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return Optional.empty();
    }
    public String getVisualizationString(){
        PullModelNode root = this.nodes.get(getRootNodeKey());
        StringBuilder sb = new StringBuilder(root.getId().toUpperCase() + "\n");
        recurForVisualizationString(root, sb, 0);
        return sb.toString();
    }

    private void recurForVisualizationString(PullModelNode currNode, StringBuilder stringBuilder, int level){
        currNode.getOptions().forEach(option -> {
            stringBuilder.append(getSpaces(level * NUM_SPACES_IN_VISUALIZATION));
            stringBuilder.append("|__");
            stringBuilder.append(option.getGraphString().toUpperCase());
            stringBuilder.append("\n");
            String childNodeKey = getKey(option.getId(), currNode.getId());
            if(this.nodes.containsKey(childNodeKey)){
                PullModelNode childNode = nodes.get(childNodeKey);
                recurForVisualizationString(childNode, stringBuilder, level+1);
            }
        });
    }

    public int getNumNodesFromNode(PullModelNode node){
        int res = 1;
        for(PullModelNodeOption option : node.getOptions()){
            String childNodeKey = getKey(option.getId(), node.getId());
            if(this.nodes.containsKey(childNodeKey)){
                PullModelNode childNode = nodes.get(childNodeKey);
                res += getNumNodesFromNode(childNode);
            }
        }
        return res;
    }

    public int getNumOptionsFromNode(PullModelNode node){
        int res = 0;
        res += node.getOptions().size();
        for(PullModelNodeOption option : node.getOptions()){
            String childNodeKey = getKey(option.getId(), node.getId());
            if(this.nodes.containsKey(childNodeKey)){
                PullModelNode childNode = nodes.get(childNodeKey);
                res += getNumOptionsFromNode(childNode);
            }
        }
        return res;
    }

    private String getSpaces(int n){
        return generate(() -> " ").limit(n).collect(joining());
    }

    public static boolean isValid(PullModelData pullModelData){
        return checkRecur(pullModelData, getKey(ROOT_NODE_ID, ROOT_NODE_PARENT_ID));
    }

    private static boolean checkRecur(PullModelData pullModelData, String pullModelNodeKey){
        if(!pullModelData.getNodes().containsKey(pullModelNodeKey)){
            return false;
        }
        PullModelNode currNode = pullModelData.getNodes().get(pullModelNodeKey);
        if(!currNode.isLeaf()){
            for(PullModelNodeOption option : currNode.getOptions()){
                if(!checkRecur(pullModelData, getKey(option.getId(), currNode.getId())))
                    return false;
            }
            return true;
        }else{
            return currNode.getOptions().size() != 0; // LEAF NODE HAS SOME OPTIONS
        }
    }

    private int getStartIndexOfLeafOptions(List<PullModelNodeOption> options){
        int i;
        for(i = options.size()-1; i >= 0; --i){
            if(!PullModelNodeOption.isLeafNodeOption(options.get(i))){
                break;
            }
        }
        return i + 1;
    }


}
