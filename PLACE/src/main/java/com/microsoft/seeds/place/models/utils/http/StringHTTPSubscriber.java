package com.microsoft.seeds.place.models.utils.http;

import java.util.logging.Logger;

public class StringHTTPSubscriber implements AysncHTTPSubscriber<String>{
    private static final Logger logger = Logger.getLogger(StringHTTPSubscriber.class.getName());

    @Override
    public void subscriber(String response, String url) {
        logger.info("RECEIVED RESPONSE FROM " + url + " : " + response);
    }
}
