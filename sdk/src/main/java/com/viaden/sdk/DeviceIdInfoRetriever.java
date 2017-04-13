package com.viaden.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.UUID;

abstract class DeviceIdInfoRetriever extends PrefsStorage {
    @NonNull
    private static final String INTERNAL_ID = "internalId";

    @NonNull
    @WorkerThread
    static DeviceId obtainInfo(@NonNull final Context context) {
        final DeviceId info = DeviceIdHolder.info;
        if (info.type != null) {
            return info;
        }
        info.advertisingId = getGoogleAdvertisingIdInfo(context);
        info.androidId = getAndroidId(context);
        info.internalId = getInternalId(context);

        if (!TextUtils.isEmpty(info.advertisingId)) {
            info.value = info.advertisingId;
            info.type = Types.ADVERTISING_ID;
        } else if (!TextUtils.isEmpty(info.androidId)) {
            info.value = info.androidId;
            info.type = Types.ANDROID_ID;
        } else {
            info.value = info.internalId;
            info.type = Types.INTERNAL_ID;
        }

        return info;
    }

    @SuppressLint("HardwareIds")
    @Nullable
    private static String getAndroidId(@NonNull final Context context) {
        final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId)) {
            return HashUtils.asMD5(androidId);
        }
        return null;
    }

    @NonNull
    private static String getInternalId(@NonNull final Context context) {
        String internalId = getPrefs(context).getString(INTERNAL_ID, null);
        if (TextUtils.isEmpty(internalId)) {
            internalId = UUID.randomUUID().toString();
            getPrefs(context).edit().putString(INTERNAL_ID, internalId).apply();
        }
        return internalId;
    }

    @Nullable
    @WorkerThread
    private static String getGoogleAdvertisingIdInfo(@NonNull final Context context) {
        try {
            final Object info = new Reflection.MethodBuilder(null, "getAdvertisingIdInfo")
                    .setStatic(Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient"))
                    .addParam(Context.class, context)
                    .execute();
            return (String) new Reflection.MethodBuilder(info, "getId").execute();
        } catch (@NonNull final Exception e) {
            return null;
        }
    }

    interface Types {
        String ANDROID_ID = "ANDROID_ID";
        String ADVERTISING_ID = "ADVERTISING_ID";
        String INTERNAL_ID = "INTERNAL_ID";
    }

    private static final class DeviceIdHolder {
        @NonNull
        private static DeviceId info = new DeviceId();
    }

    static class DeviceId {
        @Nullable
        String advertisingId;
        @Nullable
        String androidId;
        @Nullable
        String internalId;
        @Nullable
        String value;
        @Nullable
        String type;
    }
}
