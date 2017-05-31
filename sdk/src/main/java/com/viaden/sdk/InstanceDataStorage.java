package com.viaden.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class InstanceDataStorage extends PrefsStorage {
    @NonNull
    private static final String PREFIX = "instanceData";

    @Nullable
    static InstanceData load(@NonNull final Context context) {
        return new InstanceData.Builder(getPrefs(context), PREFIX).build();
    }

    static void save(@NonNull final Context context, @NonNull final InstanceData instanceData) {
        final SharedPreferences.Editor editor = getPrefs(context).edit();
        instanceData.toPrefs(editor, PREFIX);
        editor.apply();
    }
}
