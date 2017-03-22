package com.viaden.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class DeviceStateReceiver extends BroadcastReceiver {
    private static final String TAG = "StateReceiver";

    @Override
    public void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onHandleIntent(" + (intent == null ? "null" : intent.toUri(Intent.URI_INTENT_SCHEME)) + ")");
        }
        context.startService(RegistrationService.buildIntent(context));
    }
}
