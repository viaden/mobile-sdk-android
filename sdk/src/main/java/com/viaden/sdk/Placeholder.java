package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Placeholder {
    @NonNull
    private final static List<String> KEYS = Arrays.asList(
            DeviceIdValueRetriever.KEY,
            DeviceIdTypeRetriever.KEY
    );
    @NonNull
    private final Context context;
    @NonNull
    private Map<String, String> placeholders;

    Placeholder(@NonNull final Context context) {
        this.context = context;
        placeholders = new HashMap<>();
    }

    void setPlaceholders(@NonNull final Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    @NonNull
    String format(@NonNull final String template) {
        final StringBuilder builder = new StringBuilder(template);
        for (String key : KEYS) {
            final String pattern = "%{" + key + "}";
            int start;
            while ((start = builder.indexOf(pattern)) != -1) {
                final String value = getValue(key);
                builder.replace(start, start + pattern.length(), value);
            }
        }
        return builder.toString();
    }

    @Nullable
    private String getValue(@NonNull final String key) {
        switch (key) {
            case DeviceIdValueRetriever.KEY:
                return DeviceIdValueRetriever.get(context);
            case DeviceIdTypeRetriever.KEY:
                return DeviceIdTypeRetriever.get(context);
            default:
                return placeholders.get(key);
        }
    }
}
