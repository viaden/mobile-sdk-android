package com.viaden.sdk.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class StringResponseContentFactory implements ResponseContentFactory<String> {

    @NonNull
    @Override
    public ResponseContent<String> create(final int statusCode, @NonNull final InputStream stream) throws IOException {
        return new StringResponseContent(stream);
    }

    private static final class StringResponseContent implements ResponseContent<String> {
        @Nullable
        private final String content;

        private StringResponseContent(@NonNull final InputStream stream) throws IOException {
            content = new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8"));
        }

        @Nullable
        public String getContent() {
            return content;
        }
    }
}
