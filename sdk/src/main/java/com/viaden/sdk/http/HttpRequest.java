package com.viaden.sdk.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The http request we send to server. Instances of this class are not immutable. The
 * request body may be consumed only once. The other fields are immutable.
 */
public final class HttpRequest {
    @NonNull
    private final String url;
    @NonNull
    private final HttpMethod httpMethod;
    @NonNull
    private final Map<String, String> headers;
    @Nullable
    private final HttpBody body;

    private HttpRequest(@NonNull final String url, @NonNull final HttpMethod httpMethod,
                        @NonNull final Map<String, String> headers,
                        @Nullable final HttpBody body) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @NonNull
    public Map<String, String> getAllHeaders() {
        return headers;
    }

    @Nullable
    public String getHeader(final String name) {
        return headers.get(name);
    }

    @Nullable
    public HttpBody getBody() {
        return body;
    }

    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        @Nullable
        private String url;
        @Nullable
        private HttpMethod httpMethod;
        @Nullable
        private Map<String, String> headers;
        @Nullable
        private HttpBody body;

        public Builder() {
        }

        public Builder(@NonNull final HttpRequest origin) {
            url = origin.url;
            httpMethod = origin.httpMethod;
            headers = new HashMap<>(origin.headers);
            body = origin.body;
        }

        @NonNull
        public Builder setUrl(@Nullable final String url) {
            this.url = url;
            return this;
        }

        @NonNull
        public Builder setHttpMethod(@Nullable final HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @NonNull
        public Builder setBody(@Nullable final HttpBody body) {
            this.body = body;
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
        public Builder addHeaders(@NonNull final Map<String, String> headers) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.putAll(headers);
            return this;
        }

        @NonNull
        public Builder setHeaders(@Nullable final Map<String, String> headers) {
            this.headers = headers == null ? null : new HashMap<>(headers);
            return this;
        }

        @NonNull
        public HttpRequest build() {
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url");
            }
            if (httpMethod == null) {
                httpMethod = HttpMethod.GET;
            }
            if (headers == null) {
                headers = Collections.emptyMap();
            }
            return new HttpRequest(url, httpMethod, headers, body);
        }
    }
}
