package ir.faravandgroup.carsa.view.activities;

import static ir.faravandgroup.carsa.business.DownloadManager.startFileDownload;
import static ir.faravandgroup.carsa.business.PermissionHelper.STORAGE_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.ServiceWorkerWebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import ir.faravandgroup.carsa.R;
import ir.faravandgroup.carsa.business.BlobDownloadBridge;
import ir.faravandgroup.carsa.business.DeviceInfoHelper;
import ir.faravandgroup.carsa.business.DownloadManager;
import ir.faravandgroup.carsa.business.deadlines.DeadlineNotificationBridge;
import ir.faravandgroup.carsa.business.PermissionHelper;

public class WebAppMainActivity extends AppCompatActivity {

    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;
    private static final String NOTIFICATION_PERMISSION_EVENT = "carsanotificationpermission";
    private volatile String currentPageUrl;
    protected WebView vw;
    protected RelativeLayout loading;
    private static final String BAZAAR_APP_PACKAGE = "com.farsitel.bazaar";
    private static final String MYKET_APP_PACKAGE = "ir.mservices.market";
    private static final String APP_DOMAIN = "mycarsa.ir";
    private static final String DEEP_LINK_PATH = "/openapp";

    protected boolean errorOccurred = false;
    protected RelativeLayout noInternetContainer;
    protected Button retrybutton;
    protected MaterialButton contactsupportbutton;
    protected ImageView splash;
    private ValueCallback<Uri[]> mUploadMessage;
    private String pendingGeolocationOrigin;
    private GeolocationPermissions.Callback pendingGeolocationCallback;
    private final String LOG_TAG = "DREG";
    protected static final String PREF_NAME = "AppUsagePrefs";
    protected static final String KEY_USAGE_DATES = "usage_dates";
    protected static final String KEY_RATING_SHOWN = "rating_shown";
    protected static final String KEY_LAST_RATING_SHOWN = "last_rating_shown";
    private static final String KEY_FIRST_OPEN_DAY = "first_open_day";
    private static final long RATING_DELAY_MILLIS = 10_000;
    private final Handler ratingHandler = new Handler(Looper.getMainLooper());
    private final Runnable ratingPrompt = this::showRating;
    private boolean ratingPromptPending;
    private boolean activityResumed;
    private boolean pageReadyForRating;
    private AlertDialog ratingDialog;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    // App URL and version methods
    protected String getSiteURL() {
        return "https://app.mycarsa.ir";
    }

    private enum Market {
        Myket(1),
        Bazaar(2),
        Direct(3);

        private final int code;

        Market(int code) {
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }
    // Change this item for the APK being distributed through each channel.
    protected final Market MarketName = Market.Bazaar;

    protected String getURLEnd() {
        return "client=app&version=" + getAppVersion() + "&brnd=" + Build.BRAND + "&mdl=" + Build.MODEL + "&dvc=" + Build.DEVICE + "&sdkv=" + Build.VERSION.SDK_INT + "&hasSms=false&mkt=" + MarketName.getCode() + "&age=" + getDaysSinceFirstOpen();
    }

    /** Adds the app query string once, keeping any #fragment at the end. */
    protected String appendURLEnd(String url) {
        if (url.contains("client=app"))
            return url;
        String fragment = "";
        int hashIndex = url.indexOf('#');
        if (hashIndex >= 0) {
            fragment = url.substring(hashIndex);
            url = url.substring(0, hashIndex);
        }
        return url + (url.contains("?") ? "&" : "?") + getURLEnd() + fragment;
    }

    protected static boolean isAppHost(String host) {
        return host != null && (host.equals(APP_DOMAIN) || host.endsWith("." + APP_DOMAIN));
    }

    private long getDaysSinceFirstOpen() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        long today = getStartOfTodayMillis();
        long firstOpenDay = prefs.getLong(KEY_FIRST_OPEN_DAY, 0);

        if (firstOpenDay == 0) {
            prefs.edit().putLong(KEY_FIRST_OPEN_DAY, today).apply();
            return 0;
        }

        return Math.max(0, (today - firstOpenDay) / MILLIS_PER_DAY);
    }

    private long getStartOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    protected int getAppVersion() {
        return DeviceInfoHelper.getAppVersion(this);
    }

    // Show webview after a delay
    private void showWebView() {
        new Handler(Looper.getMainLooper()).postDelayed(this::showWebViewNow, 3000);
    }

    private void showWebViewNow() {
        vw.setVisibility(View.VISIBLE);
        splash.setVisibility(View.GONE);
        noInternetContainer.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
    }

    private void showLoading() {
        vw.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        splash.setVisibility(View.GONE);
        noInternetContainer.setVisibility(View.GONE);
    }

    // Show no internet UI after a delay
    private void showNoInternet() {
        pageReadyForRating = false;
        cancelRatingPrompt();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            vw.setVisibility(View.GONE);
            splash.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            noInternetContainer.setVisibility(View.VISIBLE);
        }, 3000);
    }

    private String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        scheduleRatingPrompt();
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        cancelRatingPrompt();
        super.onPause();
    }

    private void scheduleRatingPrompt() {
        if (!activityResumed || !pageReadyForRating || ratingPromptPending || ratingDialog != null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String today = getCurrentDate();
        if (prefs.getBoolean(KEY_RATING_SHOWN, false)
                || today.equals(prefs.getString(KEY_LAST_RATING_SHOWN, ""))) {
            return;
        }

        Set<String> usageDates = new HashSet<>(prefs.getStringSet(KEY_USAGE_DATES, new HashSet<>()));
        if (usageDates.add(today)) {
            prefs.edit().putStringSet(KEY_USAGE_DATES, usageDates).apply();
        }
        if (usageDates.size() % 3 == 0) {
            ratingPromptPending = ratingHandler.postDelayed(ratingPrompt, RATING_DELAY_MILLIS);
        }
    }

    private void cancelRatingPrompt() {
        ratingHandler.removeCallbacks(ratingPrompt);
        ratingPromptPending = false;
        if (ratingDialog != null) {
            ratingDialog.dismiss();
            ratingDialog = null;
        }
    }

    private void showRating() {
        ratingPromptPending = false;
        if (!activityResumed || !pageReadyForRating || isFinishing() || isDestroyed() || ratingDialog != null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String today = getCurrentDate();
        if (prefs.getBoolean(KEY_RATING_SHOWN, false)
                || today.equals(prefs.getString(KEY_LAST_RATING_SHOWN, ""))
                || !prefs.getStringSet(KEY_USAGE_DATES, new HashSet<>()).contains(today)) {
            return;
        }

        ratingDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rate_app_title)
                .setMessage(R.string.rate_app_message)
                .setNegativeButton(R.string.rate_app_later, null)
                .setPositiveButton(R.string.rate_app_now, (dialog, which) -> {
                    dialog.dismiss();
                    if (navigateToRating()) {
                        prefs.edit().putBoolean(KEY_RATING_SHOWN, true).apply();
                    }
                })
                .create();
        ratingDialog.setOnDismissListener(dialog -> {
            if (ratingDialog == dialog) {
                ratingDialog = null;
            }
        });
        ratingDialog.show();
        prefs.edit().putString(KEY_LAST_RATING_SHOWN, today).apply();
    }

    // Initiate the WebView with settings
    @SuppressLint("SetJavaScriptEnabled")
    protected void initiateWebView() {
        var activity = this;
        WebSettings webSettings = vw.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setGeolocationEnabled(true);

        // Service Worker / PWA offline support
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceWorkerController swController = ServiceWorkerController.getInstance();
            swController.setServiceWorkerClient(new ServiceWorkerClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return null; // Let service worker handle it normally
                }
            });
            ServiceWorkerWebSettings swSettings = swController.getServiceWorkerWebSettings();
            swSettings.setAllowContentAccess(true);
            swSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + "; sweetwebappclient " + getAppVersion());

        vw.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        updateCacheMode();
        registerNetworkReceiver();
        vw.setWebChromeClient(new PQChromeClient());
        vw.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Iframes (e.g. silent token refresh) navigate on their own.
                if (!request.isForMainFrame())
                    return false;
                return handleUrlLoading(view, request.getUrl().toString());
            }

            // Fallback for older versions
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlLoading(view, url);
            }

            private boolean handleUrlLoading(WebView view, String url) {
                Log.d("url", url);
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

                if (scheme.equals("blob")) {
                    fetchBlobAndDownload(url, activity, vw);
                    return true;
                }
                if (url.contains("sweetsoft/command/clearwebcache")) {
                    vw.clearCache(true);
                    vw.reload();
                    return true;
                }
                if (url.contains("sweetsoft/command/exitapp")) {
                    finish();
                    return true;
                }
                if (url.contains("sweetsoft/command/")) {
                    // Commands of other Sweet apps (e.g. SMS screens) have no native screen here.
                    return true;
                }
                // tel:, mailto:, sms:, geo:, intent: and market links belong to other apps.
                if (!scheme.equals("http") && !scheme.equals("https")) {
                    openExternally(url);
                    return true;
                }
                if (url.endsWith(".jpg") || url.endsWith(".png")) {
                    showPhoto(uri);
                    return true;
                }
                if (url.contains("openmode=openinbrowser") || isExternalAppLink(uri.getHost())) {
                    openExternally(url);
                    return true;
                }
                if (isAppHost(uri.getHost())) {
                    String fullUrl = appendURLEnd(url);
                    if (!fullUrl.equals(url)) {
                        view.loadUrl(fullUrl);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Only show no-internet for MAIN frame errors, not sub-resources
                if (failingUrl != null && failingUrl.equals(vw.getUrl())) {
                    if (!isNetworkAvailable()) {
                        // Try loading from cache first
                        vw.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                        vw.reload(); // Retry with cache mode
                    }
                    showNoInternet();
                }
                errorOccurred = true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!errorOccurred) {
                    showWebViewNow();
                    pageReadyForRating = isAppHost(Uri.parse(url).getHost());
                    scheduleRatingPrompt();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorOccurred = false;
                currentPageUrl = url;
                pageReadyForRating = false;
                cancelRatingPrompt();
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                currentPageUrl = url;
            }
        });
        vw.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Log.d(LOG_TAG, "Download started: URL=" + url + ", MIME=" + mimeType);
            if (url.startsWith("blob:")) {
                fetchBlobAndDownload(url, activity, vw);
            } else if (url.startsWith("data:")) {
                if (PermissionHelper.hasStoragePermission(activity)) {
                    DownloadManager.handleDataUriDownload(url, mimeType, activity);
                } else {
                    requestStoragePermission(url, mimeType);
                }
            } else if (url.startsWith("http:") || url.startsWith("https:")) {
                if (PermissionHelper.hasStoragePermission(activity)) {
                    startFileDownload(url, contentDisposition, mimeType, activity);
                } else {
                    requestStoragePermission(url, mimeType);
                }
            }
        });
        vw.setLongClickable(false);
        vw.setHapticFeedbackEnabled(false);
        vw.setOnLongClickListener(v -> true);
        vw.addJavascriptInterface(new BlobDownloadBridge(activity), "BlobBridge");
        vw.addJavascriptInterface(new DeadlineNotificationBridge(activity, () -> currentPageUrl,
                Uri.parse(getSiteURL()).getHost(), this::requestNotificationPermission), DeadlineNotificationBridge.JS_NAME);

        showLoading();
        vw.loadUrl(getStartURL(getIntent()));

        retrybutton.setOnClickListener(v -> {
            errorOccurred = false;
            showWebView();
            vw.reload();
        });
        contactsupportbutton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(WebAppMainActivity.this);
            builder.setTitle("پشتیبانی");
            builder.setMessage("برای پشتیبانی و پیشنهادات و انتقادات می توانید با شماره تلفن 09367356253 از طریق پیامک یا پیام رسان بله در ارتباط باشید.");
            builder.setPositiveButton("بستن", (dialog, which) -> dialog.dismiss());
            builder.setCancelable(true);
            builder.show();
        });
    }

    /**
     * Start page for a launch intent. A link like https://mycarsa.ir/openapp/garage?id=5
     * opens /garage?id=5 inside the app; anything else opens the home page.
     */
    protected String getStartURL(@Nullable Intent intent) {
        String path = "/";
        Uri data = intent == null ? null : intent.getData();
        if (data != null && isAppHost(data.getHost()) && data.getPath() != null && data.getPath().startsWith(DEEP_LINK_PATH)) {
            String rest = data.getPath().substring(DEEP_LINK_PATH.length());
            if (rest.startsWith("/"))
                path = rest;
            if (data.getEncodedQuery() != null)
                path += "?" + data.getEncodedQuery();
        }
        return appendURLEnd(getSiteURL() + path);
    }

    /** Hosts that have their own app (maps, messengers, markets) open outside the WebView. */
    private static boolean isExternalAppLink(String host) {
        if (host == null)
            return false;
        String[] externalHosts = {"map.ir", "maps.google.com", "www.google.com", "goo.gl", "neshan.org",
                "api.whatsapp.com", "wa.me", "t.me", "ble.ir", "eitaa.com", "instagram.com",
                "cafebazaar.ir", "myket.ir"};
        for (String external : externalHosts) {
            if (host.equals(external) || host.endsWith("." + external))
                return true;
        }
        return false;
    }

    private void openExternally(String url) {
        try {
            Intent intent;
            if (url.startsWith("intent:")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
            } else if (url.startsWith("tel:")) {
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "برنامه ای برای باز کردن این لینک پیدا نشد.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot open external link: " + e.getMessage());
        }
    }

    private void fetchBlobAndDownload(String blobUrl, Activity activity, WebView vw) {

        String jsCode =
                "fetch('" + blobUrl + "')" +
                        ".then(response => Promise.all([response.blob(), response.headers.get('Content-Type')]))" +
                        ".then(([blob, headerMime]) => {" +
                        "    const mime = headerMime || blob.type || 'application/octet-stream';" +
                        "    return new Promise((resolve, reject) => {" +
                        "        const reader = new FileReader();" +
                        "        reader.onloadend = () => resolve({ dataUri: reader.result, mimeType: mime });" +
                        "        reader.onerror = () => reject('FileReader error');" +
                        "        reader.readAsDataURL(blob);" +
                        "    });" +
                        "})" +
                        ".then(result => {" +
                        "    BlobBridge.onBlobReady(result.dataUri, result.mimeType);" +
                        "})" +
                        ".catch(err => {" +
                        "    BlobBridge.onBlobError(err ? err.toString() : 'Unknown error');" +
                        "});";

        vw.evaluateJavascript(jsCode, null);
    }

    private void requestStoragePermission(String url, String mimeType) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "کارسا برای ذخیره اطلاعات درخواستی در گوشی شما به دسترسی فضای ذخیره سازی نیاز دارد.", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            // For Android 10+, proceed without permission
            if (url.startsWith("data:")) {
                DownloadManager.handleDataUriDownload(url, mimeType, this);
            } else {
                startFileDownload(url, null, mimeType, this);
            }
        }
    }

    private void OpenAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        showWebView();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            return;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "لطفا دسترسی به فضای ذخیره سازی را در تنظیمات مربوط به کارسا در تنظیمات گوشی فعال کنید.", Toast.LENGTH_LONG).show();
                    OpenAppSettings();
                } else {
                    Toast.makeText(this, "دسترسی به فضای ذخیره سازی وجود ندارد، به همین دلیل دانلود انجام نشد.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (vw != null)
                vw.evaluateJavascript("window.dispatchEvent(new Event('" + NOTIFICATION_PERMISSION_EVENT + "'))", null);
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = hasLocationPermission();
            if (pendingGeolocationCallback != null) {
                pendingGeolocationCallback.invoke(pendingGeolocationOrigin, granted, false);
            }
            pendingGeolocationCallback = null;
            pendingGeolocationOrigin = null;
            if (!granted) {
                Toast.makeText(this, "برای نمایش موقعیت شما روی نقشه، دسترسی به موقعیت مکانی لازم است.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (vw.canGoBack()) {
            vw.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void showPhoto(Uri photoUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(photoUri, "image/*");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            vw.loadUrl(photoUri.toString());
        }
    }

    // ChromeClient to handle file chooser and geolocation
    public class PQChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
            }
            mUploadMessage = filePathCallback;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            String type = acceptTypes != null && acceptTypes.length > 0 && !acceptTypes[0].isEmpty() ? acceptTypes[0] : "*/*";
            // Extensions like ".pdf" are not MIME types; fall back to any file.
            intent.setType(type.contains("/") ? type : "*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "انتخاب فایل"), FILE_CHOOSER_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return false;
            }
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (!isAppHost(Uri.parse(origin).getHost())) {
                callback.invoke(origin, false, false);
                return;
            }
            if (hasLocationPermission()) {
                callback.invoke(origin, true, false);
                return;
            }
            pendingGeolocationOrigin = origin;
            pendingGeolocationCallback = callback;
            ActivityCompat.requestPermissions(WebAppMainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean navigateToRating() {
        String appPackage = getPackageName();
        if (openRatingLink("bazaar://details?id=" + appPackage, BAZAAR_APP_PACKAGE)
                || openRatingLink("myket://comment?id=" + appPackage, MYKET_APP_PACKAGE)
                || openRatingLink("https://cafebazaar.ir/app/" + appPackage, null)) {
            return true;
        }

        Toast.makeText(this, R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show();
        return false;
    }

    private boolean openRatingLink(String url, @Nullable String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(packageName);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) { // Multiple files selected
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) { // Single file selected
                    results = new Uri[]{data.getData()};
                }
            }
            // Always answer, otherwise the WebView never opens the chooser again after a cancel.
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(results);
                mUploadMessage = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*For Offline Support*/

    private void updateCacheMode() {
        if (isNetworkAvailable()) {
            vw.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            // Use cache when offline
            vw.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    protected BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateCacheMode();
            if (isNetworkAvailable() && errorOccurred) {
                errorOccurred = false;
                vw.reload();
            }
        }
    };

    private void registerNetworkReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    /*End Of For Offline Support*/
}
