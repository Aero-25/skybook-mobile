package com.trueskyventures.skybook;

import android.app.Application;

import com.onesignal.OneSignal;
import com.onesignal.common.threading.Continue;
import com.onesignal.debug.LogLevel;

/**
 * Application entry point. Initialises OneSignal so this device registers as a
 * push subscriber and can receive new-booking notifications even when the app
 * is closed. The App ID is public (safe to ship); the REST API key lives only
 * on the SkyBook backend.
 */
public class SkyBookApp extends Application {

    private static final String ONESIGNAL_APP_ID = "335a7927-cac2-43b6-a901-66f8d23d901d";

    @Override
    public void onCreate() {
        super.onCreate();

        OneSignal.getDebug().setLogLevel(LogLevel.NONE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // Ask for the Android 13+ notification permission (no-op on older versions).
        OneSignal.getNotifications().requestPermission(true, Continue.none());

        // Tapping a booking notification opens that booking inside the app's WebView.
        OneSignal.getNotifications().addClickListener(event -> {
            String url = null;
            try {
                org.json.JSONObject data = event.getNotification().getAdditionalData();
                if (data != null) url = data.optString("open_url", null);
            } catch (Throwable ignored) { }
            MainActivity.openBookingUrl(url);
        });
    }
}
