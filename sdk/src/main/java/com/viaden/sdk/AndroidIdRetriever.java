package com.viaden.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

final class AndroidIdRetriever {

    @SuppressLint("HardwareIds")
    @Nullable
    static String execute(@NonNull final Context context) {
        final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId)) {
            return HashUtils.asMD5(androidId);
        }
        return null;
    }
}
