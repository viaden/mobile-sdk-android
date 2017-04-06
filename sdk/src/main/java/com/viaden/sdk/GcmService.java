package com.viaden.sdk;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

public class GcmService extends GcmListenerService {

    @Nullable
    private static Command parseCommand(@Nullable final Bundle data) {
        if (data == null) {
            return null;
        }
        final String message = data.getString("message");
        if (TextUtils.isEmpty(message)) {
            return null;
        }
        final JSONObject json;
        try {
            json = new JSONObject(message);
        } catch (@NonNull final JSONException e) {
            return null;
        }
        return new Command.Builder(json).build();
    }

    @Override
    public void onMessageReceived(@Nullable final String from, @Nullable final Bundle data) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onMessageReceived(" + from + ", " + data + ")");
        }
        final Command command = parseCommand(data);
        if (command == null) {
            return;
        }
        startService(ProcessService.buildIntent(this, command));
    }
}
