package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

class Platform {
    @NonNull
    final static List<String> KEYS = Arrays.asList(
            DeviceIdValueRetriever.KEY,
            DeviceIdTypeRetriever.KEY
    );

    @Nullable
    static String get(@NonNull final Context context, @NonNull final String key) {
        switch (key) {
            case DeviceIdValueRetriever.KEY:
                return DeviceIdValueRetriever.get(context);
            case DeviceIdTypeRetriever.KEY:
                return DeviceIdTypeRetriever.get(context);
            default:
                return null;
        }
    }
}
