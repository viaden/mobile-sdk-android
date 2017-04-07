package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

class DeviceIdValueRetriever extends DeviceIdInfoRetriever {
    static final String KEY = "device.id.value";

    @Nullable
    @WorkerThread
    static String get(@NonNull final Context context) {
        final DeviceId deviceId = obtainInfo(context);
        return deviceId.value;
    }
}
