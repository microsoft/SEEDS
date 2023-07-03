package com.microsoft.seeds.place.mongoconverter;

import com.microsoft.seeds.place.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.place.models.fsm.PullModelMenuAudioFiles;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

public class PullModelMenuAudioFilesWritingConverter implements Converter<PullModelMenuAudioFiles, Document> {
    @Override
    public Document convert(PullModelMenuAudioFiles source) {
        JSONObject json = source.serialize();
        return Document.parse( json.toString() );
    }
}
