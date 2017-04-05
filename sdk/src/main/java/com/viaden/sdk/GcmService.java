package com.viaden.sdk;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class GcmService extends GcmListenerService {

    @Override
    public void onMessageReceived(@NonNull final String from, @NonNull final Bundle data) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onMessageReceived(" + from + ", " + data + ")");
        }
        startService(ProccessService.buildIntent(this, data));
    }
}
