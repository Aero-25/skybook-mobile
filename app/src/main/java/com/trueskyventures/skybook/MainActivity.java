package com.trueskyventures.skybook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://skybook-8rd.pages.dev/booking-admin.html";

    // Live instance + a URL parked for a cold start from a tapped notification.
    private static MainActivity instance;
    private static String pendingUrl;

    private WebView webView;
    private View splashView;
    private View offlineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        webView     = findViewById(R.id.webView);
        splashView  = findViewById(R.id.splashView);
        offlineView = findViewById(R.id.offlineView);
        Button retryButton = findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> loadApp());
        setupWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            splashView.setVisibility(View.GONE);
        } else {
            String startUrl = pendingUrl;
            pendingUrl = null;
            loadUrlOrDefault(startUrl);
        }
    }

    /** Called from the OneSignal notification-click listener to open a booking in-app. */
    public static void openBookingUrl(String url) {
        if (url == null || url.isEmpty()) return;
        MainActivity a = instance;
        if (a != null && a.webView != null) {
            a.runOnUiThread(() -> a.webView.loadUrl(url));
        } else {
            pendingUrl = url; // app not ready yet — load it once onCreate runs
        }
    }

    @Override
    protected void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Tag the user-agent so the web admin can tell it's already running inside
        // the app and suppress the "Install the app" banner.
        s.setUserAgentString(s.getUserAgentString() + " SkyBookApp");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                offlineView.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                fadeOut(splashView);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    splashView.setVisibility(View.GONE);
                    offlineView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
    }

    private void loadApp() {
        loadUrlOrDefault(null);
    }

    private void loadUrlOrDefault(String url) {
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            splashView.setAlpha(1f);
            splashView.setVisibility(View.VISIBLE);
            webView.loadUrl(url != null && !url.isEmpty() ? url : TARGET_URL);
        } else {
            splashView.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
        }
    }

    private void fadeOut(View view) {
        view.animate()
            .alpha(0f)
            .setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1f);
                }
            });
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
