package com.microsoft.seeds.place.models.utils.http;

import org.springframework.http.MediaType;

import java.util.logging.Logger;

public class HTTPRequest {

    private static final Logger logger = Logger.getLogger(HTTPRequest.class.getName());
    private String url;
    private MediaType contentType;
    private MediaType accept;
    private Class bodyClass;
    private Object body;
    private Class responseClass;
    private AysncHTTPSubscriber aysncHTTPSubscriber;

    public HTTPRequest(String url, Class bodyClass, Object body) {
        this.url = url;
        this.bodyClass = bodyClass;
        this.body = body;
        this.contentType = MediaType.APPLICATION_JSON;
        this.accept = MediaType.APPLICATION_JSON;
        this.responseClass = String.class;
        this.aysncHTTPSubscriber = new StringHTTPSubscriber();
    }

    public String getUrl() {
        return url;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public MediaType getAccept() {
        return accept;
    }

    public Class getBodyClass() {
        return bodyClass;
    }

    public Object getBody() {
        return body;
    }

    public Class getResponseClass() {
        return responseClass;
    }

    public AysncHTTPSubscriber getAysncHTTPSubscriber() {
        return aysncHTTPSubscriber;
    }
}
