package com.viaden.sdk;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class InstanceData {
    @NonNull
    final String id;
    @NonNull
    private final String application;
    @NonNull
    private final TokenStore tokens;
    private final long creationTime;
    private final long expiredMillis;

    @VisibleForTesting
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
    JSONObject asJson() throws JSONException {
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

    static class Builder {
        @Nullable
        private String id;
        @Nullable
        private String application;
        @Nullable
        private TokenStore.Builder tokenStore;
        @Nullable
        private Long creationTime;
        @Nullable
        private Long expiredMillis;

        public Builder() {
        }

        Builder(@NonNull final InstanceID instanceId, @NonNull final String packageName, @NonNull final String projectId) {
            final String gcmToken;
            final String fcmToken;
            try {
                gcmToken = instanceId.getToken(projectId, "GCM");
                fcmToken = instanceId.getToken(projectId, "FCM");
            } catch (@NonNull final IOException e) {
                if (Log.isLoggable(BuildConfig.LOG_TAG, Log.ERROR)) {
                    Log.e(BuildConfig.LOG_TAG, "Failed to retrieve instanceId", e);
                }
                return;
            }
            id = instanceId.getId();
            application = packageName;
            tokenStore = new TokenStore.Builder()
                    .addToken(new Token.Builder().setToken(gcmToken).setAuthorizedEntity(projectId).setScope("GCM"))
                    .addToken(new Token.Builder().setToken(fcmToken).setAuthorizedEntity(projectId).setScope("FCM"));
            creationTime = instanceId.getCreationTime();
            expiredMillis = SystemClock.uptimeMillis() + 24 * 3600 * 1000;
        }

        Builder(@NonNull final SharedPreferences prefs, @NonNull final String prefix) {
            id = prefs.getString(prefix + "." + Keys.INSTANCE_ID, null);
            application = prefs.getString(prefix + "." + Keys.APPLICATION, null);
            tokenStore = new TokenStore.Builder(prefs, prefix + "." + Keys.TOKENS);
            if (prefs.contains(prefix + "." + Keys.CREATION_TIME)) {
                creationTime = prefs.getLong(prefix + "." + Keys.CREATION_TIME, 0L);
            }
            if (prefs.contains(prefix + "." + Keys.EXPIRED_MILLIS)) {
                expiredMillis = prefs.getLong(prefix + "." + Keys.EXPIRED_MILLIS, 0L);
            }
        }

        @Nullable
        InstanceData build() {
            if (TextUtils.isEmpty(id)) {
                return null;
            }
            if (TextUtils.isEmpty(application)) {
                return null;
            }
            if (tokenStore == null) {
                tokenStore = new TokenStore.Builder();
            }
            if (creationTime == null) {
                creationTime = 0L;
            }
            if (expiredMillis == null) {
                expiredMillis = 0L;
            }
            return new InstanceData(id, application, tokenStore.build(), creationTime, expiredMillis);
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
            Builder addToken(@NonNull final Token.Builder token) {
                if (tokens == null) {
                    tokens = new ArrayList<>(1);
                }
                tokens.add(token);
                return this;
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

    @VisibleForTesting
    static class Token {
        @Nullable
        private final String token;
        @NonNull
        private final String authorizedEntity;
        @Nullable
        private final String scope;

        @VisibleForTesting
        Token(@Nullable final String token, @NonNull final String authorizedEntity, @Nullable final String scope) {
            this.token = token;
            this.authorizedEntity = authorizedEntity;
            this.scope = scope;
        }

        @NonNull
        Token.Builder newBuilder() {
            return new Token.Builder(this);
        }

        @NonNull
        private JSONObject asJson() throws JSONException {
            return new JSONObject()
                    .put(Token.Keys.TOKEN, token)
                    .put(Token.Keys.AUTHORIZED_ENTITY, authorizedEntity)
                    .put(Token.Keys.SCOPE, scope);
        }

        void toPrefs(@NonNull final SharedPreferences.Editor prefs, @NonNull final String prefix) {
            prefs.putString(prefix + "." + Token.Keys.TOKEN, token);
            prefs.putString(prefix + "." + Token.Keys.AUTHORIZED_ENTITY, authorizedEntity);
            prefs.putString(prefix + "." + Token.Keys.SCOPE, scope);
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

        static class Builder {
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
                token = prefs.getString(prefix + "." + Token.Keys.TOKEN, null);
                authorizedEntity = prefs.getString(prefix + "." + Token.Keys.AUTHORIZED_ENTITY, null);
                scope = prefs.getString(prefix + "." + Token.Keys.SCOPE, null);
            }

            @NonNull
            Token.Builder setToken(@Nullable final String token) {
                this.token = token;
                return this;
            }

            @NonNull
            Token.Builder setAuthorizedEntity(@Nullable final String authorizedEntity) {
                this.authorizedEntity = authorizedEntity;
                return this;
            }

            @NonNull
            Token.Builder setScope(@Nullable final String scope) {
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
