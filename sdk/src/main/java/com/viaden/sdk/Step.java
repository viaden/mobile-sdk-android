package com.viaden.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.viaden.sdk.http.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Step implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<Step> CREATOR = new ParcelableCreator();
    @NonNull
    final Headers headers;
    @NonNull
    final HttpMethod httpMethod;
    @NonNull
    final Uri url;
    @NonNull
    final JSONObject body;
    final long delayMillis;

    private Step(@NonNull final Headers headers, @NonNull final HttpMethod httpMethod, @NonNull final Uri url, @NonNull final JSONObject body,
                 final long delayMillis) {
        this.headers = headers;
        this.httpMethod = httpMethod;
        this.url = url;
        this.body = body;
        this.delayMillis = delayMillis;
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
        private HttpMethod httpMethod;
        @Nullable
        private Uri url;
        @Nullable
        private JSONObject body;
        @Nullable
        private Long delayMillis;

        Builder(@NonNull final JSONObject json) {
            headers = parseHeaders(json);
            httpMethod = parseHttpMethod(json);
            url = parseUrl(json);
            body = json.optJSONObject("body");
            delayMillis = json.optLong("delayMillis");
        }

        private Builder(@NonNull final Parcel p) {
            this.headers = new Headers.Builder(p);
            this.httpMethod = HttpMethod.parse(p.readString());
            this.url = Uri.CREATOR.createFromParcel(p);
            this.body = parseJsonObject(p);
            this.delayMillis = p.readLong();
        }

        private Builder(@NonNull final Step origin) {
            headers = origin.headers.newBuilder();
            httpMethod = origin.httpMethod;
            url = origin.url;
            body = origin.body;
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
            if (httpMethod == null) {
                httpMethod = HttpMethod.POST;
            }
            if (url == null) {
                return null;
            }
            if (body == null) {
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

}
