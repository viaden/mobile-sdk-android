package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.UUID;

final class InternalIdRetriever extends PrefsStorage {
    private static final String INTERNAL_ID = "internalId";

    @NonNull
    static String execute(@NonNull final Context context) {
        String internalId = getPrefs(context).getString(INTERNAL_ID, null);
        if (TextUtils.isEmpty(internalId)) {
            internalId = UUID.randomUUID().toString();
            getPrefs(context).edit().putString(INTERNAL_ID, internalId).apply();
        }
        return internalId;
    }
}
