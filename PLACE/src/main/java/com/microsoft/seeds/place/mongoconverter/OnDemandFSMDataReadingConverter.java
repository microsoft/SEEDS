package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class OnDemandFSMDataReadingConverter implements Converter<Document, OnDemandFSMData> {
    @Override
    public OnDemandFSMData convert(Document source) {
        JSONObject json = new JSONObject(source.toJson());
        return OnDemandFSMData.deserialize(json);
    }
}
