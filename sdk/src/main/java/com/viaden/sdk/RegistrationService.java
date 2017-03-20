package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegistrationService extends IntentService {
    private static final String TAG = "RegService";

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
            new InstanceData(InstanceID.getInstance(this)).store(FirebaseDatabase.getInstance().getReference());
        } catch (@NonNull final Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }

    private static class InstanceData {
        @NonNull
        private final String id;
        @NonNull
        private final String gcmToken;
        private final long creationTime;

        private InstanceData(@NonNull final InstanceID instanceId) throws IOException {
            id = instanceId.getId();
            gcmToken = instanceId.getToken("", GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            creationTime = instanceId.getCreationTime();
        }

        @NonNull
        private Map<String, Object> asMap() {
            final HashMap<String, Object> result = new HashMap<>();
            result.put("instanceId", id);
            result.put("gcmToken", gcmToken);
            result.put("creationTime", creationTime);
            return result;
        }

        void store(@NonNull final DatabaseReference database) {
            database.child("instance").child(id).updateChildren(asMap());
        }
    }
}
