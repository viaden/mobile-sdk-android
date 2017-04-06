package com.viaden.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

class Header implements Parcelable {
    @NonNull
    public static final Creator<Header> CREATOR = new ParcelableCreator();
    @NonNull
    final String name;
    @NonNull
    final String value;

    private Header(@NonNull final String name, @NonNull final String value) {
        this.name = name;
        this.value = value;
    }

    private Header(@NonNull final Parcel p) {
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

    static class Builder {
        @Nullable
        private final String name;
        @Nullable
        private final String value;

        Builder(@NonNull final JSONObject json) {
            name = json.optString("name");
            value = json.optString("value");
        }

        private Builder(@NonNull final Header origin) {
            this.name = origin.name;
            this.value = origin.value;
        }

        @Nullable
        Header build() {
            if (name == null) {
                return null;
            }
            if (value == null) {
                return null;
            }
            return new Header(name, value);
        }
    }

    private static class ParcelableCreator implements Creator<Header> {
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
