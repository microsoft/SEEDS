package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.pullmodel.PullModelData;
import com.microsoft.seeds.place.service.PullModelService;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

public class PullModelDataReadingConverter implements Converter<Document, PullModelData> {
    @Override
    public PullModelData convert(Document source) {
        JSONObject json = new JSONObject(source.toJson());
        Optional<PullModelData> pullModelDataOptional = PullModelData.fromJSON(json);
        if(pullModelDataOptional.isPresent()){
            return pullModelDataOptional.get();
        }
        System.out.println("******** ERROR : SAVED PULLMODELDATA IS NOT VALID! ******");
        return null;
    }
}
