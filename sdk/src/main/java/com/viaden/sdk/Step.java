package com.viaden.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.viaden.sdk.http.ByteArrayHttpBody;
import com.viaden.sdk.http.HttpMethod;
import com.viaden.sdk.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Step implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<Step> CREATOR = new ParcelableCreator();
    final long delayMillis;
    @NonNull
    private final Headers headers;
    @NonNull
    private final HttpMethod httpMethod;
    @NonNull
    private final Uri url;
    @NonNull
    private final JSONObject body;

    private Step(@NonNull final Headers headers, @NonNull final HttpMethod httpMethod, @NonNull final Uri url, @NonNull final JSONObject body,
                 final long delayMillis) {
        this.headers = headers;
        this.httpMethod = httpMethod;
        this.url = url;
        this.body = body;
        this.delayMillis = delayMillis;
    }

    @NonNull
    HttpRequest asHttpRequest() throws UnsupportedEncodingException {
        return new HttpRequest.Builder()
                .setHeaders(headers.asMap())
                .setHttpMethod(httpMethod)
                .setUrl(url.toString())
                .setBody(new ByteArrayHttpBody(body.toString(), "application/json"))
                .build();
    }

    @NonNull
    Builder newBuilder() {
        return new Builder(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel p, final int flags) {
        headers.writeToParcel(p);
        p.writeString(httpMethod.name());
        p.writeParcelable(url, flags);
        p.writeString(body.toString());
        p.writeLong(this.delayMillis);
    }

    static class Builder {
        @Nullable
        private Headers.Builder headers;
        @Nullable
        private JSONObject body;
        @Nullable
        private HttpMethod httpMethod;
        @Nullable
        private Uri url;
        @Nullable
        private Long delayMillis;

        Builder(@NonNull final JSONObject json) {
            headers = parseHeaders(json);
            body = json.optJSONObject("body");
            httpMethod = parseHttpMethod(json);
            url = parseUrl(json);
            delayMillis = json.optLong("delayMillis");
        }

        private Builder(@NonNull final Parcel p) {
            this.headers = new Headers.Builder(p);
            this.httpMethod = HttpMethod.parse(p.readString());
            this.body = parseJsonObject(p);
            this.url = Uri.CREATOR.createFromParcel(p);
            this.delayMillis = p.readLong();
        }

        private Builder(@NonNull final Step origin) {
            headers = origin.headers.newBuilder();
            body = origin.body;
            httpMethod = origin.httpMethod;
            url = origin.url;
            delayMillis = origin.delayMillis;
        }

        @Nullable
        private static Headers.Builder parseHeaders(final @NonNull JSONObject json) {
            final JSONArray value = json.optJSONArray("headers");
            return value == null ? null : new Headers.Builder(value);
        }

        @Nullable
        private static HttpMethod parseHttpMethod(final @NonNull JSONObject json) {
            final String value = json.optString("method");
            return value == null ? null : HttpMethod.parse(value);
        }

        @Nullable
        private static JSONObject parseJsonObject(final @NonNull Parcel p) {
            try {
                return new JSONObject(p.readString());
            } catch (@NonNull final JSONException e) {
                return null;
            }
        }

        @Nullable
        private static Uri parseUrl(final @NonNull JSONObject json) {
            final String value = json.optString("url");
            return value == null ? null : Uri.parse(value);
        }

        @Nullable
        Step build() {
            if (headers == null) {
                headers = new Headers.Builder();
            }
            if (body == null) {
                return null;
            }
            if (httpMethod == null) {
                httpMethod = HttpMethod.POST;
            }
            if (url == null) {
                return null;
            }
            if (delayMillis == null || delayMillis < 0) {
                delayMillis = 0L;
            }
            return new Step(headers.build(), httpMethod, url, body, delayMillis);
        }
    }

    private static class ParcelableCreator implements Parcelable.Creator<Step> {
        @Nullable
        @Override
        public Step createFromParcel(@NonNull final Parcel p) {
            return new Step.Builder(p).build();
        }

        @NonNull
        @Override
        public Step[] newArray(final int size) {
            return new Step[size];
        }
    }

    private static class Headers {
        @NonNull
        private final List<Headers.Header> headers;

        Headers() {
            this(Collections.<Header>emptyList());
        }

        Headers(@NonNull final List<Header> headers) {
            this.headers = Collections.unmodifiableList(headers);
        }

        @NonNull
        private Map<String, String> asMap() {
            if (headers.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<String, String> map = new HashMap<>(headers.size());
            for (final Headers.Header header : headers) {
                map.put(header.name, header.value);
            }
            return map;
        }

        @NonNull
        private Builder newBuilder() {
            return new Builder(this);
        }

        private void writeToParcel(@NonNull final Parcel p) {
            p.writeTypedList(headers);
        }

        private static class Builder {
            @Nullable
            private List<Headers.Header.Builder> builders;

            private Builder() {
            }

            private Builder(@NonNull final Parcel p) {
                final List<Headers.Header> headers = p.createTypedArrayList(Headers.Header.CREATOR);
                if (headers != null && !headers.isEmpty()) {
                    builders = new ArrayList<>(headers.size());
                    for (final Header header : headers) {
                        if (header != null) {
                            builders.add(header.newBuilder());
                        }
                    }
                }
            }

            private Builder(@NonNull final JSONArray json) {
                final int length = json.length();
                builders = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    final JSONObject value = json.optJSONObject(i);
                    if (value != null) {
                        builders.add(new Headers.Header.Builder(value));
                    }
                }
            }

            private Builder(@NonNull final Headers origin) {
                if (!origin.headers.isEmpty()) {
                    builders = new ArrayList<>(origin.headers.size());
                    for (final Header header : origin.headers) {
                        builders.add(header.newBuilder());
                    }
                }
            }

            @NonNull
            private Headers build() {
                if (builders == null || builders.isEmpty()) {
                    return new Headers();
                }
                final List<Headers.Header> headers = new ArrayList<>(builders.size());
                for (final Headers.Header.Builder builder : builders) {
                    final Headers.Header header = builder.build();
                    if (header != null) {
                        headers.add(header);
                    }
                }
                return new Headers(headers);
            }
        }

        private static class Header implements Parcelable {
            @NonNull
            public static final Creator<Header> CREATOR = new ParcelableCreator();
            @NonNull
            private final String name;
            @NonNull
            private final String value;

            Header(@NonNull final String name, @NonNull final String value) {
                this.name = name;
                this.value = value;
            }

            Header(@NonNull final Parcel p) {
                this.name = p.readString();
                this.value = p.readString();
            }

            @NonNull
            Builder newBuilder() {
                return new Builder(this);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull final Parcel p, final int flags) {
                p.writeString(name);
                p.writeString(value);
            }

            private static class Builder {
                @Nullable
                private final String name;
                @Nullable
                private final String value;

                private Builder(@NonNull final JSONObject json) {
                    name = json.optString("name");
                    value = json.optString("value");
                }

                private Builder(@NonNull final Header origin) {
                    this.name = origin.name;
                    this.value = origin.value;
                }

                @Nullable
                private Header build() {
                    if (name == null) {
                        return null;
                    }
                    if (value == null) {
                        return null;
                    }
                    return new Header(name, value);
                }
            }

            private static class ParcelableCreator implements Parcelable.Creator<Header> {
                @NonNull
                @Override
                public Header createFromParcel(@NonNull final Parcel p) {
                    return new Header(p);
                }

                @NonNull
                @Override
                public Header[] newArray(final int size) {
                    return new Header[size];
                }
            }
        }
    }
}
