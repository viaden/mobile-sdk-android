package com.viaden.sdk.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The http response we receive from server. Instances of this class are not immutable. The
 * response body may be consumed only once. The other fields are immutable.
 */
public class HttpResponse {
    private final int statusCode;
    private final long totalSize;
    @Nullable
    private final String reasonPhrase;
    @NonNull
    private final Map<String, String> headers;
    @Nullable
    private final String contentType;
    @NonNull
    private final ResponseContent<?> responseContent;

    private HttpResponse(@NonNull final ResponseContent<?> responseContent, final int statusCode, final long totalSize,
                         @Nullable final String reasonPhrase, @NonNull final Map<String, String> headers,
                         @Nullable final String contentType) {
        this.responseContent = responseContent;
        this.statusCode = statusCode;
        this.totalSize = totalSize;
        this.reasonPhrase = reasonPhrase;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.contentType = contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response content of the {@code HttpResponse}.
     *
     * @return The response content of the {@code HttpResponse}.
     */
    @Nullable
    public <T> T getContent() {
        return (T) responseContent.getContent();
    }

    /**
     * Returns the size of the {@code HttpResponse}'s body. {@code -1} if the size of the
     * {@code HttpResponse}'s body is unknown.
     *
     * @return The size of the {@code HttpResponse}'s body.
     */
    public long getTotalSize() {
        return totalSize;
    }

    @Nullable
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Nullable
    public String getContentType() {
        return contentType;
    }

    @Nullable
    public String getHeader(@NonNull final String name) {
        return headers.get(name);
    }

    @NonNull
    public Map<String, String> getAllHeaders() {
        return headers;
    }

    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        @Nullable
        private Integer statusCode;
        @Nullable
        private ResponseContent<?> responseContent;
        @Nullable
        private Long totalSize;
        @Nullable
        private String reasonPhrase;
        @Nullable
        private Map<String, String> headers;
        @Nullable
        private String contentType;

        public Builder() {
        }

        public Builder(@NonNull final HttpResponse origin) {
            statusCode = origin.statusCode;
            responseContent = origin.responseContent;
            totalSize = origin.totalSize;
            reasonPhrase = origin.reasonPhrase;
            headers = new HashMap<>(origin.headers);
            contentType = origin.contentType;
        }

        @NonNull
        public Builder setResponseContent(@Nullable final ResponseContent<?> responseContent) {
            this.responseContent = responseContent;
            return this;
        }

        @NonNull
        public Builder setStatusCode(@Nullable final Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @NonNull
        public Builder setTotalSize(@Nullable final Integer totalSize) {
            this.totalSize = totalSize == null ? null : totalSize.longValue();
            return this;
        }

        @NonNull
        public Builder setReasonPhrase(@Nullable final String reasonPhrase) {
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        @NonNull
        public Builder setHeaders(@Nullable final Map<String, String> headers) {
            this.headers = headers == null ? null : new HashMap<>(headers);
            return this;
        }

        @NonNull
        public Builder addHeaders(@NonNull final Map<String, String> headers) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.putAll(headers);
            return this;
        }

        @NonNull
        public Builder addHeader(@NonNull final String name, @NonNull final String value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(name, value);
            return this;
        }

        @NonNull
        public Builder setContentType(@Nullable final String contentType) {
            this.contentType = contentType;
            return this;
        }

        @NonNull
        public HttpResponse build() {
            if (statusCode == null) {
                statusCode = 0;
            }
            if (totalSize == null) {
                totalSize = -1L;
            }
            if (headers == null) {
                headers = Collections.emptyMap();
            }
            if (responseContent == null) {
                responseContent = new EmptyResponseContent();
            }
            return new HttpResponse(responseContent, statusCode, totalSize, reasonPhrase, headers, contentType);
        }

        private static class EmptyResponseContent implements ResponseContent<Object> {
            @Nullable
            @Override
            public Object getContent() {
                return null;
            }
        }
    }
}
