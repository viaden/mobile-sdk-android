package com.viaden.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class Platform {
    @NonNull
    final static List<String> KEYS = Arrays.asList(
            "device.vendor",
            "device.model",
            "os.version",
            DeviceIdValueRetriever.KEY,
            DeviceIdTypeRetriever.KEY,
            UserAgentRetriever.KEY,
            DisplaySizeWidthPixelsRetriever.KEY,
            DisplaySizeHeightPixelsRetriever.KEY,
            ScreenOrientationRetriever.KEY
    );

    @Nullable
    public static String get(@NonNull final Context context, @NonNull final String key) {
        switch (key) {
            case "device.vendor":
                return Build.MANUFACTURER;
            case "device.model":
                return Build.MODEL;
            case "os.version":
                return Build.VERSION.RELEASE;
            case DeviceIdValueRetriever.KEY:
                return DeviceIdValueRetriever.get(context);
            case DeviceIdTypeRetriever.KEY:
                return DeviceIdTypeRetriever.get(context);
            case UserAgentRetriever.KEY:
                return UserAgentRetriever.get(context);
            case DisplaySizeWidthPixelsRetriever.KEY:
                return DisplaySizeWidthPixelsRetriever.get(context);
            case DisplaySizeHeightPixelsRetriever.KEY:
                return DisplaySizeHeightPixelsRetriever.get(context);
            case ScreenOrientationRetriever.KEY:
                return ScreenOrientationRetriever.get(context);
            default:
                return null;
        }
    }

    abstract static class DeviceIdInfoRetriever extends PrefsStorage {
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

    @VisibleForTesting
    static class DeviceIdValueRetriever extends DeviceIdInfoRetriever {
        static final String KEY = "device.id.value";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final DeviceId deviceId = obtainInfo(context);
            return deviceId.value;
        }
    }

    @VisibleForTesting
    static class DeviceIdTypeRetriever extends DeviceIdInfoRetriever {
        static final String KEY = "device.id.type";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final DeviceId deviceId = obtainInfo(context);
            return deviceId.type;
        }
    }

    private static class UserAgentRetriever {
        @NonNull
        static final String KEY = "user.agent";
        @Nullable
        private static String useragent = "";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final CountDownLatch latch = new CountDownLatch(1);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        useragent = new WebView(context).getSettings().getUserAgentString();
                    } catch (@NonNull final Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await(3, TimeUnit.SECONDS);
            } catch (@NonNull final InterruptedException ignored) {
            }
            return useragent;
        }
    }

    private static class DisplaySizeWidthPixelsRetriever extends DisplaySizeRetriever {
        static final String KEY = "device.display.width";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final DisplaySize displaySize = obtainInfo(context);
            return String.valueOf(displaySize.widthPixels);
        }
    }

    private static class DisplaySizeHeightPixelsRetriever extends DisplaySizeRetriever {
        static final String KEY = "device.display.height";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final DisplaySize displaySize = obtainInfo(context);
            return String.valueOf(displaySize.heightPixels);
        }
    }

    private abstract static class DisplaySizeRetriever {

        @NonNull
        @WorkerThread
        static DisplaySize obtainInfo(@NonNull final Context context) {
            final DisplaySize displaySize = DisplaySizeHolder.info;
            if (displaySize.ready) {
                return displaySize;
            }
            final Point point = getDisplaySize(context);
            displaySize.widthPixels = point.x;
            displaySize.heightPixels = point.y;
            displaySize.ready = true;

            return displaySize;
        }

        @NonNull
        private static Point getDisplaySize(@NonNull final Context context) {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return getDisplaySizeJellyBean(display);
            }
            final Point point = new Point();
            display.getSize(point);
            return point;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private static Point getDisplaySizeJellyBean(@NonNull final Display display) {
            final Point point = new Point();
            display.getRealSize(point);
            return point;
        }

        private static final class DisplaySizeHolder {
            @NonNull
            private static DisplaySize info = new DisplaySize();
        }

        static class DisplaySize {
            int widthPixels;
            int heightPixels;
            boolean ready;
        }
    }

    private static class ScreenOrientationRetriever {
        @NonNull
        static final String KEY = "device.screen.orientation";

        @Nullable
        @WorkerThread
        static String get(@NonNull final Context context) {
            final Configuration configuration = context.getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return "LANDSCAPE";
            } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                return "PORTRAIT";
            } else {
                return "UNKNOWN";
            }
        }
    }
}
