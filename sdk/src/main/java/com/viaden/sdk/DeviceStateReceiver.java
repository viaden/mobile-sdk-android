package com.viaden.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class DeviceStateReceiver extends BroadcastReceiver {

    private static boolean hasBroadcastAction(@Nullable final String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_DATE_CHANGED.equals(action) ||
                Intent.ACTION_TIME_CHANGED.equals(action) ||
                Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
                Intent.ACTION_POWER_CONNECTED.equals(action) ||
                Intent.ACTION_USER_PRESENT.equals(action);
    }

    @Override
    public void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onHandleIntent(" + (intent == null ? "null" : intent.toUri(Intent.URI_INTENT_SCHEME)) + ")");
        }
        if (intent == null || !hasBroadcastAction(intent.getAction())) {
            return;
        }
        context.startService(RegistrationService.buildIntent(context));
    }
}
