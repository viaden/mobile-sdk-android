package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.viaden.sdk.http.ByteArrayHttpBody;
import com.viaden.sdk.http.HttpMethod;
import com.viaden.sdk.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProccessService extends IntentService {

    public ProccessService() {
        super(BuildConfig.LOG_TAG);
    }

    @NonNull
    static Intent buildIntent(@NonNull final Context context, @NonNull final Bundle data) {
        return new Intent(context, ProccessService.class).putExtras(data);
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onHandleIntent()");
        }
        if (intent == null) {
            return;
        }
        final String message = intent.getStringExtra("message");
        if (TextUtils.isEmpty(message)) {
            return;
        }
        final JSONObject json;
        try {
            json = new JSONObject(message);
        } catch (@NonNull final JSONException e) {
            return;
        }
        final Command command = new Command.Builder(json).build();
    }

    private static class Command {
        @NonNull
        private final List<Step> steps;

        private Command(@NonNull final List<Step> steps) {
            this.steps = Collections.unmodifiableList(steps);
        }

        private void execute() {
            for (final Step step : steps) {
                step.execute();
            }
        }

        private static class Builder {
            @Nullable
            private Steps.Builder steps;

            private Builder(@NonNull final JSONObject json) {
                steps = parseSteps(json);
            }

            @Nullable
            private static Steps.Builder parseSteps(final @NonNull JSONObject json) {
                final JSONArray value = json.optJSONArray("steps");
                return value == null ? null : new Steps.Builder(value);
            }

            @Nullable
            private Command build() {
                if (steps == null) {
                    steps = new Steps.Builder();
                }
                return new Command(steps.build());
            }
        }
    }

    private static class Steps {

        private static class Builder {
            @Nullable
            private List<Step.Builder> builders;

            private Builder() {
            }

            private Builder(@NonNull final JSONArray json) {
                final int length = json.length();
                builders = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    final JSONObject value = json.optJSONObject(i);
                    if (value != null) {
                        builders.add(new Step.Builder(value));
                    }
                }
            }

            @NonNull
            private List<Step> build() {
                if (builders == null || builders.isEmpty()) {
                    return Collections.emptyList();
                }
                final List<Step> steps = new ArrayList<>(builders.size());
                for (final Step.Builder builder : builders) {
                    final Step step = builder.build();
                    if (step != null) {
                        steps.add(step);
                    }
                }
                return steps;
            }
        }
    }

    private static class Step {
        @NonNull
        private final Map<String, String> headers;
        @NonNull
        private final HttpMethod httpMethod;
        @NonNull
        private final Uri url;
        @NonNull
        private final JSONObject body;
        private final long delayMillis;

        private Step(@NonNull final Map<String, String> headers, @NonNull final HttpMethod httpMethod, @NonNull final Uri url, @NonNull final JSONObject body,
                     final long delayMillis) {
            this.headers = Collections.unmodifiableMap(headers);
            this.httpMethod = httpMethod;
            this.url = url;
            this.body = body;
            this.delayMillis = delayMillis;
        }

        private void execute() {
            try {
                new HttpRequest.Builder()
                        .setHeaders(headers)
                        .setHttpMethod(httpMethod)
                        .setUrl(url.toString())
                        .setBody(new ByteArrayHttpBody(body.toString(), "application/json"))
                        .build();
            } catch (@NonNull final UnsupportedEncodingException ignored) {
            }
        }

        private static class Builder {
            @Nullable
            private Headers.Builder headers;
            @Nullable
            private JSONObject body;
            @Nullable
            private HttpMethod httpMethod;
            @Nullable
            private Uri url;
            @Nullable
            private Long delayMillis;

            private Builder(@NonNull final JSONObject json) {
                headers = parseHeaders(json);
                body = json.optJSONObject("body");
                httpMethod = parseHttpMethod(json);
                url = parseUrl(json);
                delayMillis = json.optLong("delayMillis");
            }

            @Nullable
            private static Headers.Builder parseHeaders(final @NonNull JSONObject json) {
                final JSONArray value = json.optJSONArray("headers");
                return value == null ? null : new Headers.Builder(value);
            }

            @Nullable
            private static HttpMethod parseHttpMethod(final @NonNull JSONObject json) {
                final String value = json.optString("method");
                return value == null ? null : HttpMethod.parse(value);
            }

            @Nullable
            private static Uri parseUrl(final @NonNull JSONObject json) {
                final String value = json.optString("url");
                return value == null ? null : Uri.parse(value);
            }

            @Nullable
            private Step build() {
                if (headers == null) {
                    headers = new Headers.Builder();
                }
                if (body == null) {
                    return null;
                }
                if (httpMethod == null) {
                    httpMethod = HttpMethod.POST;
                }
                if (url == null) {
                    return null;
                }
                if (delayMillis == null || delayMillis < 0) {
                    delayMillis = 0L;
                }
                return new Step(headers.build(), httpMethod, url, body, delayMillis);
            }
        }
    }

    private static class Headers {

        private static class Builder {
            @Nullable
            private List<Header.Builder> builders;

            private Builder() {
            }

            private Builder(@NonNull final JSONArray json) {
                final int length = json.length();
                builders = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    final JSONObject value = json.optJSONObject(i);
                    if (value != null) {
                        builders.add(new Header.Builder(value));
                    }
                }
            }

            @NonNull
            private Map<String, String> build() {
                if (builders == null || builders.isEmpty()) {
                    return Collections.emptyMap();
                }
                final Map<String, String> headers = new HashMap<>(builders.size());
                for (final Header.Builder builder : builders) {
                    final Header header = builder.build();
                    if (header != null) {
                        headers.put(header.name, header.value);
                    }
                }
                return headers;
            }
        }
    }

    private static class Header {
        @NonNull
        private final String name;
        @NonNull
        private final String value;

        private Header(@NonNull final String name, @NonNull final String value) {
            this.name = name;
            this.value = value;
        }

        private static class Builder {
            @Nullable
            private final String name;
            @Nullable
            private final String value;

            private Builder(@NonNull final JSONObject json) {
                name = json.optString("name");
                value = json.optString("value");
            }

            @Nullable
            private Header build() {
                if (name == null) {
                    return null;
                }
                if (value == null) {
                    return null;
                }
                return new Header(name, value);
            }
        }
    }
}
