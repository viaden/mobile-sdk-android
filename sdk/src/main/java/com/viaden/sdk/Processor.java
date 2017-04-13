package com.viaden.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.viaden.sdk.http.ByteArrayHttpBody;
import com.viaden.sdk.http.HttpClient;
import com.viaden.sdk.http.HttpRequest;
import com.viaden.sdk.http.HttpResponse;
import com.viaden.sdk.script.FSException;
import com.viaden.sdk.script.FScript;
import com.viaden.sdk.script.NullObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

class Processor {
    @NonNull
    private final HttpClient httpClient;
    @NonNull
    private final Dispatcher dispatcher;
    @NonNull
    private final Placeholder placeholder;
    @NonNull
    private final ProcessorScript script;

    Processor(@NonNull final Context context) {
        this(new HttpClient(), Dispatcher.from(context), new Placeholder(context));
    }

    @VisibleForTesting
    Processor(@NonNull final HttpClient httpClient, @NonNull final Dispatcher dispatcher, @NonNull final Placeholder placeholder) {
        this(httpClient, dispatcher, placeholder, new ProcessorScript(placeholder));
    }

    private Processor(@NonNull final HttpClient httpClient, @NonNull final Dispatcher dispatcher, @NonNull final Placeholder placeholder,
                      @NonNull final ProcessorScript script) {
        this.httpClient = httpClient;
        this.dispatcher = dispatcher;
        this.placeholder = placeholder;
        this.script = script;
    }

    void process(@NonNull final Command command) throws IOException, FSException {
        if (command.steps.isEmpty()) {
            return;
        }
        placeholder.setPlaceholders(command.placeholders);
        final List<Step> steps = new ArrayList<>(command.steps);
        do {
            final Step step = steps.get(0);
            if (step.delayMillis > 0) {
                dispatcher.schedule(command.newBuilder().setPlaceholders(placeholder.getPlaceholders()).setSteps(steps).build(), step.delayMillis);
                break;
            }
            if (!TextUtils.isEmpty(step.requestScript)) {
                script.run(step.requestScript, null);
            }
            final HttpRequest request = new HttpRequest.Builder()
                    .setHeaders(step.headers.asMap())
                    .setHttpMethod(step.httpMethod)
                    .setUrl(placeholder.format(step.url.toString()))
                    .setBody(new ByteArrayHttpBody(placeholder.format(step.body.toString()), "application/json"))
                    .build();
            final HttpResponse httpResponse = httpClient.execute(request);
            final int statusCode = httpResponse.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                break;
            }
            if (!TextUtils.isEmpty(step.responseScript)) {
                script.run(step.responseScript, httpResponse.<String>getContent());
            }
            steps.remove(step);
        } while (!steps.isEmpty());
    }

    private static class DispatcherHandler extends Handler {
        static final int ACTION_START_SERVICE = 0;
        @NonNull
        private final Context context;

        DispatcherHandler(@NonNull final Context context) {
            super(Looper.getMainLooper());
            this.context = context;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case ACTION_START_SERVICE:
                    @SuppressWarnings("unchecked")
                    final Command command = (Command) msg.obj;
                    context.startService(ProcessService.buildIntent(context, command));
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    }

    @VisibleForTesting
    abstract static class Dispatcher {

        @NonNull
        static Dispatcher from(@NonNull final Context context) {
            final DispatcherHandler handler = new DispatcherHandler(context);
            final AlarmManagerCompat alarmManager = AlarmManagerCompat.from(context);
            if (alarmManager == null) {
                return new HandlerDispatcher(handler);
            }
            return new AlarmDispatcher(context, handler, alarmManager);
        }

        abstract boolean schedule(@NonNull final Command command, final long delayMillis);
    }

    private static class HandlerDispatcher extends Dispatcher {
        private static final long HANDLER_MAX_DELAY_MILLIS = 2L * 60L * 1000L; // 2 minutes
        private static final long HANDLER_MIN_DELAY_MILLIS = 500L;
        @NonNull
        private final DispatcherHandler handler;

        private HandlerDispatcher(@NonNull final DispatcherHandler handler) {
            this.handler = handler;
        }

        @CallSuper
        @Override
        boolean schedule(@NonNull final Command command, final long delayMillis) {
            if (delayMillis >= HANDLER_MAX_DELAY_MILLIS) {
                return false;
            }
            if (handler.hasMessages(DispatcherHandler.ACTION_START_SERVICE)) {
                return true;
            }
            final Message message = handler.obtainMessage(DispatcherHandler.ACTION_START_SERVICE, command);
            return handler.sendMessageDelayed(message, Math.max(HANDLER_MIN_DELAY_MILLIS, delayMillis));
        }
    }

    private static class AlarmDispatcher extends HandlerDispatcher {
        private static final long ALARM_MIN_DELAY_MILLIS = 2L * 60L * 1000L; // 2 minutes
        @NonNull
        private final Context context;
        @NonNull
        private final AlarmManagerCompat alarmManager;

        private AlarmDispatcher(@NonNull final Context context, @NonNull final DispatcherHandler handler, @NonNull final AlarmManagerCompat alarmManager) {
            super(handler);
            this.context = context;
            this.alarmManager = alarmManager;
        }

        @Override
        boolean schedule(@NonNull final Command command, final long delayMillis) {
            if (super.schedule(command, delayMillis)) {
                return true;
            }
            final long millis = System.currentTimeMillis() + Math.max(ALARM_MIN_DELAY_MILLIS, delayMillis);
            return alarmManager.scheduleAlarm(ProcessService.buildIntent(context, command), millis);
        }
    }

    private static class ProcessorScript extends FScript {
        @NonNull
        private final Placeholder placeholder;
        @Nullable
        private String content;

        private ProcessorScript(@NonNull Placeholder placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public Object getVar(final String name) throws FSException {
            if ("content".equalsIgnoreCase(name)) {
                return content == null ? "" : content;
            }
            return super.getVar(name);
        }

        @Override
        public Object callFunction(final String name, final Vector params) throws FSException {
            if ("set".equalsIgnoreCase(name) && params.size() == 2) {
                final String key = (String) params.get(0);
                final String value = (String) params.get(1);
                placeholder.setPlaceholders(Collections.singletonMap(key, value));
            } else if ("get".equalsIgnoreCase(name) && params.size() == 1) {
                final String key = (String) params.get(0);
                return placeholder.getPlaceholder(key);
            } else {
                return super.callFunction(name, params);
            }
            return new NullObject();
        }

        private void run(@NonNull final String script, @Nullable final String content) throws IOException, FSException {
            this.content = content;
            reset();
            load(new ByteArrayInputStream(script.getBytes(Charset.forName("UTF-8"))));
            run();
        }
    }
}
