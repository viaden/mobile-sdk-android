package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.viaden.sdk.http.HttpClient;
import com.viaden.sdk.http.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessService extends IntentService {
    @NonNull
    private static final String ACTION_EXECUTE = "com.viaden.sdk.ACTION_PROCESS";
    @NonNull
    private static final String EXTRA_COMMAND = "command";

    public ProcessService() {
        super(BuildConfig.LOG_TAG + "_Process");
    }

    @NonNull
    static Intent buildIntent(@NonNull final Context context, @NonNull final Command command) {
        return new Intent(context, ProcessService.class).setAction(ACTION_EXECUTE).putExtra(EXTRA_COMMAND, command);
    }

    @Nullable
    private static Command parseCommand(@Nullable final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getParcelableExtra(EXTRA_COMMAND);
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (Log.isLoggable(BuildConfig.LOG_TAG, Log.DEBUG)) {
            Log.d(BuildConfig.LOG_TAG, "onHandleIntent()");
        }
        final Command command = parseCommand(intent);
        if (command == null || command.steps.isEmpty()) {
            return;
        }
        final HttpClient httpClient = new HttpClient();
        final Dispatcher dispatcher = Dispatcher.from(this);
        final List<Step> steps = new ArrayList<>(command.steps);
        do {
            final Step step = steps.get(0);
            if (step.delayMillis > 0) {
                final Command newCommand = command.newBuilder().setSteps(steps).build();
                if (newCommand != null) {
                    dispatcher.schedule(newCommand, step.delayMillis);
                }
                break;
            }
            try {
                final HttpResponse httpResponse = httpClient.execute(step.asHttpRequest());
                final int statusCode = httpResponse.getStatusCode();
                if (statusCode < 400 || statusCode == 422) {
                    break;
                }
            } catch (@NonNull final IOException e) {
                break;
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
                    context.startService(buildIntent(context, command));
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    }

    private abstract static class Dispatcher {

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
            return alarmManager.scheduleAlarm(buildIntent(context, command), millis);
        }
    }
}
