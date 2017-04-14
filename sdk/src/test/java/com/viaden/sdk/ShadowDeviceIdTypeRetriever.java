package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Platform.DeviceIdTypeRetriever.class)
public class ShadowDeviceIdTypeRetriever {
    static String value = "fake_device_id_type";

    @Implementation
    public static String get(@NonNull final Context context) {
        return value;
    }
}
