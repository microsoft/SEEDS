package com.microsoft.seeds.place;

import com.microsoft.seeds.place.mongoconverter.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class Converters {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {

        return new MongoCustomConversions(
                Arrays.asList(
                        new ExpFSMReadingConverter(),
                        new ExpFSMWritingConverter(),
                        new OnDemandFSMDataReadingConverter(),
                        new OnDemandFSMDataWritingConverter(),
                        new PullModelMenuAudioFilesReadingConverter(),
                        new PullModelMenuAudioFilesWritingConverter(),
                        new RawDataReadingConverter(),
                        new RawDataWritingConverter(),
                        new PullModelDataReadingConverter(),
                        new PullModelDataWritingConverter()));
    }
}
