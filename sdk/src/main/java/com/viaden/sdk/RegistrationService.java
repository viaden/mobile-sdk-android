package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;
import com.viaden.sdk.http.ByteArrayHttpBody;
import com.viaden.sdk.http.HttpClient;
import com.viaden.sdk.http.HttpMethod;
import com.viaden.sdk.http.HttpRequest;
import com.viaden.sdk.http.HttpResponse;

import org.json.JSONException;

import java.io.IOException;

public class RegistrationService extends IntentService {

    public RegistrationService() {
        super(BuildConfig.LOG_TAG);
    }

    @NonNull
    static Intent buildIntent(@NonNull final Context context) {
        return new Intent(context, RegistrationService.class);
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onHandleIntent()");
        }
        final String projectId = new MetaDataRetriever(getPackageName(), getPackageManager()).get("viadenSenderId");
        final String endpoint = new MetaDataRetriever(getPackageName(), getPackageManager()).get("viadenEndpointId");
        if (TextUtils.isEmpty(projectId) || TextUtils.isEmpty(endpoint)) {
            if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                Log.e(BuildConfig.LOG_TAG, "Failed to load meta-data");
            }
            return;
        }
        final InstanceData savedInstanceData = InstanceDataStorage.load(this);
        if (savedInstanceData != null && !savedInstanceData.isExpired()) {
            return;
        }
        final InstanceData instanceData = new InstanceData.Builder(InstanceID.getInstance(this), getPackageName(), projectId).build();
        if (instanceData == null) {
            if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                Log.e(BuildConfig.LOG_TAG, "Failed to load instanceId data");
            }
            return;
        }
        InstanceDataStorage.save(this, instanceData);
        if (instanceData == savedInstanceData) {
            return;
        }
        try {
            final Uri uri = Uri.parse("https://" + endpoint + ".firebaseio.com").buildUpon()
                    .appendEncodedPath("instances")
                    .appendEncodedPath(instanceData.id + ".json")
                    .build();
            if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
                Log.d(BuildConfig.LOG_TAG, uri.toString());
                Log.d(BuildConfig.LOG_TAG, instanceData.asJson().toString());
            }
            final HttpResponse response = new HttpClient().execute(new HttpRequest.Builder()
                    .setHttpMethod(HttpMethod.PUT)
                    .setUrl(uri.toString())
                    .setBody(new ByteArrayHttpBody(instanceData.asJson().toString(), "application/json"))
                    .build());
            if (response.getStatusCode() != 200) {
                if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                    Log.e(BuildConfig.LOG_TAG, "Server response status [" + response.getStatusCode() + "] " + response.getReasonPhrase());
                }
            }
        } catch (@NonNull final IOException | JSONException e) {
            if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                Log.e(BuildConfig.LOG_TAG, "Failed to save instanceId data", e);
            }
        }
    }

    private static class MetaDataRetriever {
        @NonNull
        private final String packageName;
        @NonNull
        private final PackageManager packageManager;
        @Nullable
        private Bundle metaData;

        private MetaDataRetriever(@NonNull final String packageName, @NonNull final PackageManager packageManager) {
            this.packageName = packageName;
            this.packageManager = packageManager;
        }

        @NonNull
        private static Bundle getMetaData(final @NonNull String packageName, final @NonNull PackageManager packageManager) {
            try {
                return packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData;
            } catch (@NonNull final PackageManager.NameNotFoundException e) {
                return Bundle.EMPTY;
            }
        }

        @Nullable
        private String get(@NonNull final String key) {
            if (metaData == null) {
                metaData = getMetaData(packageName, packageManager);
            }
            final String value = metaData.getString(key);
            return value != null && value.startsWith("x") ? value.substring(1) : null;
        }
    }
}
