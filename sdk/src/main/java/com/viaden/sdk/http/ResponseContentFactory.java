package com.viaden.sdk.http;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

interface ResponseContentFactory<T> {
    @NonNull
    ResponseContent<T> create(int statusCode, @NonNull InputStream stream) throws IOException;
}
