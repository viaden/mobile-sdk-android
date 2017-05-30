package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
            return;
        }
        InstanceDataStorage.save(this, instanceData);
        if (instanceData == savedInstanceData) {
            return;
        }
        if (savedInstanceData == null) {
            if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                Log.e(BuildConfig.LOG_TAG, "Failed to load instanceId data");
            }
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

    private static class InstanceDataStorage extends PrefsStorage {
        @NonNull
        private static final String PREFIX = "instanceData";

        @Nullable
        static InstanceData load(@NonNull final Context context) {
            return new InstanceData.Builder(getPrefs(context), PREFIX).build();
        }

        static void save(@NonNull final Context context, @NonNull final InstanceData instanceData) {
            final SharedPreferences.Editor editor = getPrefs(context).edit();
            instanceData.toPrefs(editor, PREFIX);
            editor.apply();
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
        private final long expiredMillis;

        InstanceData(@NonNull final String id, @NonNull final String application, @NonNull final List<Token> tokens, final long creationTime,
                     final long expiredMillis) {
            this.id = id;
            this.application = application;
            this.tokens = new TokenStore(tokens);
            this.creationTime = creationTime;
            this.expiredMillis = expiredMillis;
        }

        boolean isExpired() {
            return expiredMillis <= SystemClock.uptimeMillis();
        }

        @NonNull
        private JSONObject asJson() throws JSONException {
            return new JSONObject()
                    .put(Keys.INSTANCE_ID, id)
                    .put(Keys.APPLICATION, application)
                    .put(Keys.TOKENS, tokens.asJson())
                    .put(Keys.CREATION_TIME, creationTime)
                    .put(Keys.EXPIRED_MILLIS, expiredMillis);
        }

        void toPrefs(@NonNull final SharedPreferences.Editor prefs, @NonNull final String prefix) {
            prefs.putString(prefix + "." + Keys.INSTANCE_ID, id);
            prefs.putString(prefix + "." + Keys.APPLICATION, application);
            tokens.toPrefs(prefs, prefix + "." + Keys.TOKENS);
            prefs.putLong(prefix + "." + Keys.CREATION_TIME, creationTime);
            prefs.putLong(prefix + "." + Keys.EXPIRED_MILLIS, expiredMillis);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InstanceData)) {
                return false;
            }
            final InstanceData that = (InstanceData) o;
            return creationTime == that.creationTime &&
                    Objects.equal(id, that.id) &&
                    Objects.equal(application, that.application) &&
                    Objects.equal(tokens, that.tokens);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id, application, tokens, creationTime, expiredMillis);
        }

        private interface Keys {
            String INSTANCE_ID = "instanceId";
            String APPLICATION = "application";
            String TOKENS = "tokens";
            String CREATION_TIME = "creationTime";
            String EXPIRED_MILLIS = "expiredTime";
        }

        private static class Builder {
            @Nullable
            private String id;
            @Nullable
            private String application;
            @Nullable
            private TokenStore.Builder tokens;
            @Nullable
            private Long creationTime;
            @Nullable
            private Long expiredMillis;

            Builder(@NonNull final InstanceID instanceId, @NonNull final String packageName, @NonNull final String projectId) {
                final String token;
                try {
                    token = instanceId.getToken(projectId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                } catch (@NonNull final IOException e) {
                    return;
                }
                id = instanceId.getId();
                application = packageName;
                tokens = new TokenStore.Builder(Collections.singletonList(new Token.Builder()
                        .setToken(token)
                        .setAuthorizedEntity(projectId)
                        .setScope(GoogleCloudMessaging.INSTANCE_ID_SCOPE)
                        .build()));
                creationTime = instanceId.getCreationTime();
                expiredMillis = SystemClock.uptimeMillis() + 24 * 3600 * 1000;
            }

            Builder(@NonNull final SharedPreferences prefs, @NonNull final String prefix) {
                id = prefs.getString(prefix + "." + Keys.INSTANCE_ID, null);
                application = prefs.getString(prefix + "." + Keys.APPLICATION, null);
                tokens = new TokenStore.Builder(prefs, prefix + "." + Keys.TOKENS);
                if (prefs.contains(prefix + "." + Keys.CREATION_TIME)) {
                    creationTime = prefs.getLong(prefix + "." + Keys.CREATION_TIME, 0L);
                }
                if (prefs.contains(prefix + "." + Keys.EXPIRED_MILLIS)) {
                    expiredMillis = prefs.getLong(prefix + "." + Keys.EXPIRED_MILLIS, 0L);
                }
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
                    tokens = new TokenStore.Builder();
                }
                if (creationTime == null) {
                    creationTime = 0L;
                }
                if (expiredMillis == null) {
                    expiredMillis = 0L;
                }
                return new InstanceData(id, application, tokens.build(), creationTime, expiredMillis);
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

        void toPrefs(@NonNull final SharedPreferences.Editor prefs, @NonNull final String prefix) {
            if (!tokens.isEmpty()) {
                prefs.putInt(prefix, tokens.size());
                for (int i = 0; i < tokens.size(); i++) {
                    tokens.get(i).toPrefs(prefs, prefix + "." + i);
                }
            }
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TokenStore)) {
                return false;
            }
            final TokenStore that = (TokenStore) o;
            return Objects.equal(tokens, that.tokens);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(tokens);
        }

        static class Builder {
            @Nullable
            private List<Token.Builder> tokens;

            Builder() {
            }

            Builder(@NonNull final SharedPreferences prefs, @NonNull final String prefix) {
                final int count = prefs.getInt(prefix, 0);
                tokens = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    tokens.add(new Token.Builder(prefs, prefix + "." + i));
                }
            }

            Builder(@NonNull final Collection<Token> list) {
                tokens = new ArrayList<>(list.size());
                for (final Token token : list) {
                    tokens.add(token.newBuilder());
                }
            }

            @NonNull
            List<Token> build() {
                if (tokens == null) {
                    return Collections.emptyList();
                }
                final List<Token> list = new ArrayList<>(tokens.size());
                for (final Token.Builder token : tokens) {
                    final Token newToken = token.build();
                    if (newToken != null) {
                        list.add(newToken);
                    }
                }
                return list;
            }
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
        Builder newBuilder() {
            return new Builder(this);
        }

        @NonNull
        private JSONObject asJson() throws JSONException {
            return new JSONObject()
                    .put(Keys.TOKEN, token)
                    .put(Keys.AUTHORIZED_ENTITY, authorizedEntity)
                    .put(Keys.SCOPE, scope);
        }

        void toPrefs(@NonNull final SharedPreferences.Editor prefs, @NonNull final String prefix) {
            prefs.putString(prefix + "." + Keys.TOKEN, token);
            prefs.putString(prefix + "." + Keys.AUTHORIZED_ENTITY, authorizedEntity);
            prefs.putString(prefix + "." + Keys.SCOPE, scope);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Token)) {
                return false;
            }
            final Token that = (Token) o;
            return Objects.equal(token, that.token) &&
                    Objects.equal(authorizedEntity, that.authorizedEntity) &&
                    Objects.equal(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(token, authorizedEntity, scope);
        }

        private interface Keys {
            String TOKEN = "token";
            String AUTHORIZED_ENTITY = "authorizedEntity";
            String SCOPE = "scope";
        }

        private static class Builder {
            @Nullable
            private String token;
            @Nullable
            private String authorizedEntity;
            @Nullable
            private String scope;

            Builder() {
            }

            public Builder(@NonNull final Token origin) {
                token = origin.token;
                authorizedEntity = origin.authorizedEntity;
                scope = origin.scope;
            }

            Builder(@NonNull final SharedPreferences prefs, @NonNull final String prefix) {
                token = prefs.getString(prefix + "." + Keys.TOKEN, null);
                authorizedEntity = prefs.getString(prefix + "." + Keys.AUTHORIZED_ENTITY, null);
                scope = prefs.getString(prefix + "." + Keys.SCOPE, null);
            }

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
