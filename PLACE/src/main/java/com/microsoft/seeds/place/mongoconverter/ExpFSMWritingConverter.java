package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMGeneratorAPI;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.data.convert.WritingConverter;
import org.springframework.core.convert.converter.Converter;

@WritingConverter
public class ExpFSMWritingConverter implements Converter<ExpFSM, Document> {
    @Override
    public Document convert(ExpFSM source) {
        JSONObject json = FSMGeneratorAPI.serialiseFSM(source);
        return Document.parse( json.toString() );
    }
}
