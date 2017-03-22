package com.viaden.sdk;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class GcmService extends GcmListenerService {
    private static final String TAG = "GcmService";

    @Override
    public void onMessageReceived(@NonNull final String from, @NonNull final Bundle data) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onMessageReceived(" + from + ", " + data + ")");
        }
    }
}
