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

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.viaden.sdk.http.ByteArrayHttpBody;
import com.viaden.sdk.http.HttpClient;
import com.viaden.sdk.http.HttpMethod;
import com.viaden.sdk.http.HttpRequest;
import com.viaden.sdk.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RegistrationService extends IntentService {
    private static final String TAG = "RegService";
    private static final String ENDPOINT = BuildConfig.VIADEN_ENDPOINT;

    public RegistrationService() {
        super(TAG);
    }

    @NonNull
    static Intent buildIntent(@NonNull final Context context) {
        return new Intent(context, RegistrationService.class);
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onHandleIntent()");
        }
        final String projectId = new MetaDataRetriever(getPackageName(), getPackageManager()).get("gcm_sender_id");
        if (TextUtils.isEmpty(projectId)) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Failed to load meta-data");
            }
            return;
        }
        final InstanceData instanceData = new InstanceRetriever(InstanceID.getInstance(this), getPackageName(), projectId).get();
        if (instanceData == null) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Failed to load instanceId data");
            }
            return;
        }
        try {
            final Uri uri = Uri.parse(ENDPOINT).buildUpon()
                    .appendEncodedPath("instances")
                    .appendEncodedPath(instanceData.id + ".json")
                    .build();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, uri.toString());
                Log.d(TAG, instanceData.asJson().toString());
            }
            final HttpResponse response = new HttpClient().execute(new HttpRequest.Builder()
                    .setHttpMethod(HttpMethod.PUT)
                    .setUrl(uri.toString())
                    .setBody(new ByteArrayHttpBody(instanceData.asJson().toString(), "application/json"))
                    .build());
            if (response.getStatusCode() != 200) {
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "Server response status [" + response.getStatusCode() + "] " + response.getReasonPhrase());
                }
            }
        } catch (@NonNull final IOException | JSONException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Failed to save instanceId data", e);
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

    private static class InstanceRetriever {
        @NonNull
        private final InstanceID instanceId;
        @NonNull
        private final String packageName;
        @NonNull
        private final String projectId;

        private InstanceRetriever(@NonNull final InstanceID instanceId, @NonNull final String packageName, @NonNull final String projectId) {
            this.instanceId = instanceId;
            this.packageName = packageName;
            this.projectId = projectId;
        }

        @Nullable
        private InstanceData get() {
            final String token;
            try {
                token = instanceId.getToken(projectId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            } catch (@NonNull final IOException e) {
                return null;
            }
            return new InstanceData.Builder()
                    .setId(instanceId.getId())
                    .setApplication(packageName)
                    .setTokens(Collections.singletonList(new Token.Builder()
                            .setToken(token)
                            .setAuthorizedEntity(projectId)
                            .setScope(GoogleCloudMessaging.INSTANCE_ID_SCOPE)
                            .build()))
                    .setCreationTime(instanceId.getCreationTime())
                    .build();
        }
    }

    private static class InstanceData {
        @NonNull
        private final String id;
        @NonNull
        private final String application;
        @NonNull
        private final TokenStore tokens;
        private final long creationTime;

        InstanceData(@NonNull final String id, @NonNull final String application, @NonNull final List<Token> tokens, final long creationTime) {
            this.id = id;
            this.application = application;
            this.tokens = new TokenStore(tokens);
            this.creationTime = creationTime;
        }

        @NonNull
        private JSONObject asJson() throws JSONException {
            return new JSONObject()
                    .put("instanceId", id)
                    .put("application", application)
                    .put("tokens", tokens.asJson())
                    .put("creationTime", creationTime);
        }

        private static class Builder {
            @Nullable
            private String id;
            @Nullable
            private String application;
            @Nullable
            private List<Token> tokens;
            @Nullable
            private Long creationTime;

            @NonNull
            private Builder setId(@Nullable final String id) {
                this.id = id;
                return this;
            }

            @NonNull
            private Builder setApplication(@Nullable final String application) {
                this.application = application;
                return this;
            }

            @NonNull
            private Builder setTokens(@Nullable final List<Token> tokens) {
                this.tokens = tokens;
                return this;
            }

            @NonNull
            private Builder setCreationTime(@Nullable final Long creationTime) {
                this.creationTime = creationTime;
                return this;
            }

            @Nullable
            private InstanceData build() {
                if (TextUtils.isEmpty(id)) {
                    return null;
                }
                if (TextUtils.isEmpty(application)) {
                    return null;
                }
                if (tokens == null) {
                    tokens = Collections.emptyList();
                }
                if (creationTime == null) {
                    creationTime = 0L;
                }
                return new InstanceData(id, application, tokens, creationTime);
            }
        }
    }

    private static class TokenStore {
        @NonNull
        private final List<Token> tokens;

        private TokenStore(@NonNull final List<Token> tokens) {
            this.tokens = Collections.unmodifiableList(tokens);
        }

        @NonNull
        private JSONArray asJson() throws JSONException {
            final JSONArray json = new JSONArray();
            for (final Token token : tokens) {
                json.put(token.asJson());
            }
            return json;
        }
    }

    private static class Token {
        @Nullable
        private final String token;
        @NonNull
        private final String authorizedEntity;
        @Nullable
        private final String scope;

        private Token(@Nullable final String token, @NonNull final String authorizedEntity, @Nullable final String scope) {
            this.token = token;
            this.authorizedEntity = authorizedEntity;
            this.scope = scope;
        }

        @NonNull
        private JSONObject asJson() throws JSONException {
            return new JSONObject()
                    .put("token", token)
                    .put("authorizedEntity", authorizedEntity)
                    .put("scope", scope);
        }

        private static class Builder {
            @Nullable
            private String token;
            @Nullable
            private String authorizedEntity;
            @Nullable
            private String scope;

            @NonNull
            private Builder setToken(@Nullable final String token) {
                this.token = token;
                return this;
            }

            @NonNull
            private Builder setAuthorizedEntity(@Nullable final String authorizedEntity) {
                this.authorizedEntity = authorizedEntity;
                return this;
            }

            @NonNull
            private Builder setScope(@Nullable final String scope) {
                this.scope = scope;
                return this;
            }

            @Nullable
            private Token build() {
                if (TextUtils.isEmpty(authorizedEntity)) {
                    return null;
                }
                return new Token(token, authorizedEntity, scope);
            }
        }
    }
}
