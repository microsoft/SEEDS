package com.microsoft.seeds.place.models.request;

import com.microsoft.seeds.place.models.utils.Constants;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

/* DELETE QUIZ REQUEST
        {
        "type": "delete-quiz",
        "id": "c9ca1b54-93c6-45c3-88af-369f4b5b4261",
        }
*/
public class DeleteQuizAzureFunctionRequest {

    public static final String DELETE_QUIZ_AZURE_FUNCTION_REQ_TYPE = "delete-quiz";
    public String type;
    public String id;

    public DeleteQuizAzureFunctionRequest() {
        type = DELETE_QUIZ_AZURE_FUNCTION_REQ_TYPE;
    }

    public DeleteQuizAzureFunctionRequest(String id) {
        this.id = id;
        type = DELETE_QUIZ_AZURE_FUNCTION_REQ_TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject toJSON(){
        JSONObject res = new JSONObject();
        res.put("type", type);
        res.put("id", id);
        return res;
    }
}
