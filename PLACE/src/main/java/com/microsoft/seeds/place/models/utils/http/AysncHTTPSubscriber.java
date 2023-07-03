package com.microsoft.seeds.place.models.utils.http;

import java.util.function.Consumer;

public interface AysncHTTPSubscriber<T> {
    public void subscriber(T response, String url);
}
