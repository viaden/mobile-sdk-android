package com.viaden.sdk;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class GcmService extends GcmListenerService {
    private static final String TAG = "GcmService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "From: " + from);
            Log.d(TAG, "Message: " + message);
        }
    }
}
