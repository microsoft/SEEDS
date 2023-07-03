package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.RawData;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class RawDataWritingConverter implements Converter<RawData, Document> {
    @Override
    public Document convert(RawData source) {
        JSONObject json = source.toJSON();
        return Document.parse( json.toString() );
    }
}
