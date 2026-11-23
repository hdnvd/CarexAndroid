package ir.faravandgroup.carsa.business.deadlines;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.util.List;
import java.util.function.Supplier;

public class DeadlineNotificationBridge {
    public static final String JS_NAME = "CarsaNotifications";
    private static final String STATE_GRANTED = "granted";
    private static final String STATE_DENIED = "denied";
    private static final String STATE_NOT_ASKED = "notAsked";

    private final Activity activity;
    private final Supplier<String> currentUrl;
    private final String allowedHost;
    private final Runnable requestPermission;

    public DeadlineNotificationBridge(Activity activity, Supplier<String> currentUrl, String allowedHost, Runnable requestPermission) {
        this.activity = activity;
        this.currentUrl = currentUrl;
        this.allowedHost = allowedHost;
        this.requestPermission = requestPermission;
    }

    @JavascriptInterface
    public void syncDeadlines(String json) {
        if (!isTrusted())
            return;
        List<DeadlineReminder> deadlines;
        try {
            deadlines = DeadlineReminder.listFromJson(json);
        } catch (JSONException e) {
            return;
        }
        Context app = activity.getApplicationContext();
        DeadlineReminderScheduler.replaceAll(app, deadlines);
        if (!deadlines.isEmpty() && needsPermissionRequest(app)) {
            DeadlineReminderStore.markPermissionAsked(app);
            activity.runOnUiThread(requestPermission);
        }
    }

    @JavascriptInterface
    public void clear() {
        if (isTrusted())
            DeadlineReminderScheduler.clear(activity.getApplicationContext());
    }

    @JavascriptInterface
    public String permissionState() {
        if (!isTrusted())
            return "";
        Context app = activity.getApplicationContext();
        if (DeadlineReminderReceiver.canNotify(app))
            return STATE_GRANTED;
        return needsPermissionRequest(app) ? STATE_NOT_ASKED : STATE_DENIED;
    }

    @JavascriptInterface
    public void openSettings() {
        if (!isTrusted())
            return;
        activity.runOnUiThread(() -> {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + activity.getPackageName()));
            }
            try {
                activity.startActivity(intent);
            } catch (Exception ignored) {
            }
        });
    }

    private boolean needsPermissionRequest(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !DeadlineReminderStore.permissionAsked(context);
    }

    private boolean isTrusted() {
        String url = currentUrl.get();
        if (url == null)
            return false;
        Uri uri = Uri.parse(url);
        return "https".equalsIgnoreCase(uri.getScheme()) && allowedHost.equalsIgnoreCase(uri.getHost());
    }
}
