package com.microsoft.seeds.place.mongoconverter;
import com.microsoft.seeds.place.models.fsm.ExpFSM;
import com.microsoft.seeds.place.models.fsm.FSMGeneratorAPI;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.core.convert.converter.Converter;

@ReadingConverter
public class ExpFSMReadingConverter implements Converter<Document, ExpFSM> {
    @Override
    public ExpFSM convert(Document source) {
        JSONObject json = new JSONObject(source.toJson());
        return FSMGeneratorAPI.deserializeFSM(json);
    }
}
