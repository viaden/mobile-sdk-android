package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

public class RegistrationService extends IntentService {
    private static final String TAG = "RegIntentService";

    public RegistrationService() {
        super(TAG);
    }

    @NonNull
    static Intent buildIntent(@NonNull final Context context) {
        return new Intent(context, RegistrationService.class);
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        try {
            final InstanceID id = InstanceID.getInstance(this);
            final String token = id.getToken("", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + token);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }
}
