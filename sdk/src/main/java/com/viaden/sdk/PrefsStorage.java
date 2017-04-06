package com.viaden.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

abstract class PrefsStorage {
    @NonNull
    private static final String PREFERENCES_NAME = "com.viaden.sdk.v1";

    @NonNull
    static SharedPreferences getPrefs(@NonNull final Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
