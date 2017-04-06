package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

class AdvertisingIdInfoRetriever {
    @NonNull
    private final Context context;

    AdvertisingIdInfoRetriever(@NonNull final Context context) {
        this.context = context;
    }

    @Nullable
    @WorkerThread
    String getAdvertisingId() {
        return obtainInfo().advertisingId;
    }

    @WorkerThread
    public boolean isLimitAdTrackingEnabled() {
        final Info info = obtainInfo();
        if (info.isLimitAdTrackingEnabled != null) {
            return info.isLimitAdTrackingEnabled;
        }
        return false;
    }

    @NonNull
    @WorkerThread
    private Info obtainInfo() {
        final Info info = InfoHolder.info;
        if (info.isLimitAdTrackingEnabled == null) {
            try {
                final Object advertisingIdInfo = new Reflection.MethodBuilder(null, "getAdvertisingIdInfo")
                        .setStatic(Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient"))
                        .addParam(Context.class, context)
                        .execute();
                info.isLimitAdTrackingEnabled = (Boolean) new Reflection.MethodBuilder(advertisingIdInfo, "isLimitAdTrackingEnabled").execute();
                info.advertisingId = (String) new Reflection.MethodBuilder(advertisingIdInfo, "getId").execute();
            } catch (@NonNull final Exception e) {
                info.isLimitAdTrackingEnabled = false;
                info.advertisingId = null;
            }
        }
        return info;
    }

    private static final class InfoHolder {
        @NonNull
        private static Info info = new Info();
    }

    private static final class Info {
        @Nullable
        private Boolean isLimitAdTrackingEnabled;
        @Nullable
        private String advertisingId;
    }

}
