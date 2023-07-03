package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.pullmodel.PullModelData;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class PullModelDataWritingConverter implements Converter<PullModelData, Document> {
    @Override
    public Document convert(PullModelData source) {
        try {
            JSONObject json = source.toJSON();
            return Document.parse( json.toString() );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
