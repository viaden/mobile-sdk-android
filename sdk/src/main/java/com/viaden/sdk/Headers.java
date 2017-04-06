package com.viaden.sdk;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Headers {
    @NonNull
    private final List<Header> headers;

    private Headers() {
        this(Collections.<Header>emptyList());
    }

    private Headers(@NonNull final List<Header> headers) {
        this.headers = Collections.unmodifiableList(headers);
    }

    @NonNull
    Map<String, String> asMap() {
        if (headers.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> map = new HashMap<>(headers.size());
        for (final Header header : headers) {
            map.put(header.name, header.value);
        }
        return map;
    }

    @NonNull
    Builder newBuilder() {
        return new Builder(this);
    }

    void writeToParcel(@NonNull final Parcel p) {
        p.writeTypedList(headers);
    }

    static class Builder {
        @Nullable
        private List<Header.Builder> builders;

        Builder() {
        }

        Builder(@NonNull final Parcel p) {
            final List<Header> headers = p.createTypedArrayList(Header.CREATOR);
            if (headers != null && !headers.isEmpty()) {
                builders = new ArrayList<>(headers.size());
                for (final Header header : headers) {
                    if (header != null) {
                        builders.add(header.newBuilder());
                    }
                }
            }
        }

        Builder(@NonNull final JSONArray json) {
            final int length = json.length();
            builders = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                final JSONObject value = json.optJSONObject(i);
                if (value != null) {
                    builders.add(new Header.Builder(value));
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
        Headers build() {
            if (builders == null || builders.isEmpty()) {
                return new Headers();
            }
            final List<Header> headers = new ArrayList<>(builders.size());
            for (final Header.Builder builder : builders) {
                final Header header = builder.build();
                if (header != null) {
                    headers.add(header);
                }
            }
            return new Headers(headers);
        }
    }

}
