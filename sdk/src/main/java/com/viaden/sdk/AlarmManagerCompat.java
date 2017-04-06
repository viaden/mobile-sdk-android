package com.viaden.sdk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

class AlarmManagerCompat {
    @NonNull
    private final Context context;
    @NonNull
    private final AlarmManagerImpl impl;

    private AlarmManagerCompat(@NonNull final Context context, @NonNull final AlarmManagerImpl impl) {
        this.context = context;
        this.impl = impl;
    }

    @Nullable
    static AlarmManagerCompat from(@NonNull final Context context) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return null;
        }
        return new AlarmManagerCompat(context, build(alarmManager));
    }

    @NonNull
    private static AlarmManagerImpl build(@NonNull final AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new AlarmManagerMarshmallowImpl(alarmManager);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new AlarmManagerKitkatImpl(alarmManager);
        } else {
            return new AlarmManagerStubImpl(alarmManager);
        }
    }

    @Nullable
    private static PendingIntent getPendingIntent(@NonNull final Context context, final Intent intent, final int flags) {
        // repeating PendingIntent with service seams to have problems
        try {
            return PendingIntent.getBroadcast(context, 0, intent, flags);
        } catch (@NonNull final Exception e) {
            // java.lang.SecurityException: Permission Denial: getIntentSender() from pid=31482, uid=10057, (need uid=-1) is not allowed
            return null;
        }
    }

    boolean scheduleAlarm(@NonNull final Intent intent, final long triggerAtMillis) {
        final PendingIntent pendingIntent = getPendingIntent(context, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        if (pendingIntent == null) {
            return false;
        }
        try {
            impl.scheduleAlarm(triggerAtMillis, pendingIntent);
            return true;
        } catch (@NonNull final Exception ignored) {
            return false;
        }
    }

    interface AlarmManagerImpl {
        void scheduleAlarm(long triggerAtMillis, @NonNull PendingIntent operation);
    }

    abstract static class AlarmManagerBaseImpl implements AlarmManagerImpl {
        @NonNull
        final AlarmManager alarmManager;

        AlarmManagerBaseImpl(@NonNull final AlarmManager alarmManager) {
            this.alarmManager = alarmManager;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static final class AlarmManagerMarshmallowImpl extends AlarmManagerBaseImpl {

        private AlarmManagerMarshmallowImpl(@NonNull final AlarmManager alarmManager) {
            super(alarmManager);
        }

        @Override
        public void scheduleAlarm(final long triggerAtMillis, @NonNull final PendingIntent pendingIntent) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static final class AlarmManagerKitkatImpl extends AlarmManagerBaseImpl {

        private AlarmManagerKitkatImpl(@NonNull final AlarmManager alarmManager) {
            super(alarmManager);
        }

        @Override
        public void scheduleAlarm(final long triggerAtMillis, @NonNull final PendingIntent pendingIntent) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static final class AlarmManagerStubImpl extends AlarmManagerBaseImpl {

        private AlarmManagerStubImpl(final AlarmManager alarmManager) {
            super(alarmManager);
        }

        @Override
        public void scheduleAlarm(final long triggerAtMillis, @NonNull final PendingIntent pendingIntent) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }
}
