package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.UUID;

abstract class DeviceIdInfoRetriever extends PrefsStorage {

    @NonNull
    @WorkerThread
    static DeviceId obtainInfo(@Nullable final Context context) {
        final DeviceId info = DeviceIdHolder.info;
        if (info.value != null) {
            return info;
        }
        if (context == null) {
            info.value = UUID.randomUUID().toString();
            info.type = Types.INTERNAL_ID;
            return info;
        }
        final String advertisingId = new AdvertisingIdInfoRetriever(context).getAdvertisingId();
        if (!TextUtils.isEmpty(advertisingId)) {
            info.value = advertisingId;
            info.type = Types.ADVERTISING_ID;
            return info;
        }
        final String androidId = AndroidIdRetriever.execute(context);
        if (!TextUtils.isEmpty(androidId)) {
            info.value = androidId;
            info.type = Types.ANDROID_ID;
            return info;
        }
        info.value = InternalIdRetriever.execute(context);
        info.type = Types.INTERNAL_ID;
        return info;
    }

    interface Types {
        String ANDROID_ID = "ANDROID_ID";
        String ADVERTISING_ID = "GAID";
        String INTERNAL_ID = "INTERNAL_ID";
    }

    private static final class DeviceIdHolder {
        @NonNull
        private static DeviceId info = new DeviceId();
    }

    static class DeviceId {
        @Nullable
        String value;
        @Nullable
        String type;
    }
}
