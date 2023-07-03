package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.PullModelMenuAudioFiles;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class PullModelMenuAudioFilesReadingConverter implements Converter<Document, PullModelMenuAudioFiles> {
    @Override
    public PullModelMenuAudioFiles convert(Document source) {
        JSONObject json = new JSONObject(source.toJson());
        return PullModelMenuAudioFiles.deserialize(json);
    }
}
