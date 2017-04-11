package com.viaden.sdk.http;

import android.support.annotation.Nullable;

public interface ResponseContent<T> {
    /**
     * Returns the response content of the {@code HttpResponse}.
     */
    @Nullable
    T getContent();
}
