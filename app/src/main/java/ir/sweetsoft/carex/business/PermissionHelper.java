package ir.sweetsoft.carex.business;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class PermissionHelper {
    private static String LOG_TAG="sweet permission";
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 100;

    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) and above, including API 34
            Log.d(LOG_TAG, "Android 10+, no storage permission needed for DownloadManager");
            return true;
        }
        // Android 7.0 (API 24) to Android 9 (API 28)
        boolean granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        Log.d(LOG_TAG, "Permission granted: " + granted);
        return granted;
    }
}
