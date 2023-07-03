package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.RawData;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class RawDataReadingConverter implements Converter<Document, RawData> {
    @Override
    public RawData convert(Document source) {
        JSONObject json = new JSONObject(source.toJson());
        return RawData.fromJSON(json);
    }
}
