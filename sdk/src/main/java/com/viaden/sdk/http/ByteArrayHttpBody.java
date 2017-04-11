package com.viaden.sdk.http;

import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ByteArrayHttpBody extends HttpBody {
    @NonNull
    private final byte[] content;
    @NonNull
    private final InputStream contentInputStream;

    public ByteArrayHttpBody(@NonNull final String content, @NonNull final String contentType) throws UnsupportedEncodingException {
        this(content.getBytes("UTF-8"), contentType);
    }

    private ByteArrayHttpBody(@NonNull final byte[] content, @NonNull final String contentType) {
        super(contentType, content.length);
        this.content = content.clone();
        contentInputStream = new ByteArrayInputStream(content);
    }

    @NonNull
    @Override
    public InputStream getContent() {
        return contentInputStream;
    }

    @Override
    public void writeTo(@NonNull final OutputStream out) throws IOException {
        out.write(content);
    }
}
