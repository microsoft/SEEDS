package com.microsoft.seeds.fsmexecutor.models.fsm;

import com.microsoft.seeds.fsmexecutor.models.fsm.actions.FSMActionList;
import com.microsoft.seeds.fsmexecutor.models.fsm.contextcontroller.FSMContextController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.logging.Logger;

public class FSMContext extends Hashtable<String, Object> implements java.io.Serializable {

    private String id;
    private State currState;
    private State firstState;
    private String userInputEp;
    private WebClient.Builder webClientBuilder;

    private String placeGetFSMByIdApi;
    private FSMAction initialAction = State.skip;

    private FSMContextController fsmContextController;

    private Stack<ExpFSM> fsmStack = new Stack<>();
    private HashMap<String, State> fsmToLastStateMap = new HashMap<>();

    private List<ClientState> clientStateListToPush = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(FSMContext.class.getName());

    public FSMContext(String id, State s, FSMContextController fsmContextController) {
        setId(id);
        setFirstState(s);
        setFsmContextController(fsmContextController);
    }

    public FSMContext(String id, State s, FSMContextController fsmContextController, FSMAction i) {
        setId(id);
        setFirstState(s);
        setInitialAction(i);
        setFsmContextController(fsmContextController);
    }
    public FSMContextController getFsmContextController() {
        return fsmContextController;
    }

    public Stack<ExpFSM> getFsmStack() {
        return fsmStack;
    }

    public HashMap<String, State> getFsmToLastStateMap() {
        return fsmToLastStateMap;
    }

    public List<ClientState> getClientStateListToPush() {
        return clientStateListToPush;
    }

    public void clearClientStateListToPush(){
        clientStateListToPush.clear();
    }

    public void addToClientStateList(ClientState state){
        clientStateListToPush.add(state);
    }

    public void setFsmContextController(FSMContextController fsmContextController) {
        this.fsmContextController = fsmContextController;
    }

    public String getPlaceGetFSMByIdApi() {
        return placeGetFSMByIdApi;
    }

    public FSMContext setPlaceGetFSMByIdApi(String placeGetFSMByIdApi) {
        this.placeGetFSMByIdApi = placeGetFSMByIdApi;
        return this;
    }
    public WebClient.Builder getWebClientBuilder() {
        return webClientBuilder;
    }


    public String getUserInputEp() {
        return userInputEp;
    }
    public FSMContext setWebClientBuilder(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        return this;
    }

    public FSMContext setUserInputEp(String userInputEp) {
        this.userInputEp = userInputEp;
        return this;
    }

    /**
     * Initialize the context. Set first state and execute initial action.
     */
    public void initialize()
    {
        initialAction.execute(this, null);
        setCurrState(firstState);
        fsmContextController.initialize(this);
        executeCurrentStateEntryAction(null);
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setCurrState(State s) { currState = s; }
    public State getCurrState() {
        return currState;
    }

    public void setFirstState(State s) { firstState = s; }
    public State getFirstState() { return firstState; }

    public void setInitialAction(FSMAction a) { initialAction = a; }


    public void executeCurrentStateExitAction(Object data){
        currState.getStateExitAction().execute(this, data);
    }

    public void executeCurrentStateEntryAction(Object data){
        currState.getStateEntryAction().execute(this, data);
    }

    public void executeFSMAbortAction(Object data){
        FSMActionList.getAbortActionList().execute(this, data);
    }

    public List<String> getEventNameList() {
        return getCurrState().getEventNames();
    }

    void printHashtable(){
        Enumeration keysEnum = this.keys();
        while(keysEnum.hasMoreElements()){
            System.out.println(keysEnum.nextElement());
        }
    }

    public void dispatch(String eventName, Object data){
        this.fsmContextController.dispatch(eventName, data, this);
    }

    public List<Event> getOpsEvents(){
        return this.fsmContextController.getOpsEvents();
    }

    public String getType(){
        return this.fsmContextController.getType();
    }

    public String getLanguage(){
        return this.fsmContextController.getLanguage();
    }
}
