package com.viaden.sdk.http;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The base interface of a http request body. It can be implemented by different class such as
 * {@code ByteArrayHttpBody} and so on.
 */
public abstract class HttpBody {
    @NonNull
    private final String contentType;
    private final long contentLength;

    public HttpBody(@NonNull final String contentType, final long contentLength) {
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    @NonNull
    public abstract InputStream getContent() throws IOException;

    public abstract void writeTo(@NonNull final OutputStream out) throws IOException;

    /**
     * Returns the number of bytes which will be written to {@code out} when {@link #writeTo} is
     * called, or {@code -1} if that count is unknown.
     *
     * @return The Content-Length of this body.
     */
    public long getContentLength() {
        return contentLength;
    }

    @NonNull
    public String getContentType() {
        return contentType;
    }
}
