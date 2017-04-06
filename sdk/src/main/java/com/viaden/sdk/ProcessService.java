package com.viaden.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
        if (command == null) {
            return;
        }
        new Processor(this).process(command);
    }
}
