package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMGeneratorAPI;
import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class OnDemandFSMDataWritingConverter implements Converter<OnDemandFSMData, Document> {
    @Override
    public Document convert(OnDemandFSMData source) {
        JSONObject json = source.serialize();
        return Document.parse( json.toString() );
    }
}
