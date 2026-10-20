package ir.sweetsoft.carex.business;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import ir.sweetsoft.carex.view.activities.MainActivity;

public class DeviceInfoHelper {
    public static void getDeviceInfo(Activity activity) {
// دریافت اطلاعات دستگاه در Java

// Manufacturer (سازنده)
        String manufacturer = Build.MANUFACTURER;  // مثال: "Xiaomi", "Samsung", "Google"

// Model (مدل)
        String model = Build.MODEL;  // مثال: "Redmi Note 10", "SM-G991B"

// Brand (برند)
        String brand = Build.BRAND;  // مثال: "xiaomi", "samsung"

// Device name
        String device = Build.DEVICE;  // مثال: "mojito"

// Android version (نسخه اندروید)
        String androidVersion = Build.VERSION.RELEASE;  // مثال: "13", "14"

// SDK version (API level)
        int sdkVersion = Build.VERSION.SDK_INT;  // مثال: 33, 34

// نمایش در Log
        Log.d("DeviceInfo",
                "Manufacturer: " + manufacturer + "\n" +
                        "Model: " + model + "\n" +
                        "Brand: " + brand + "\n" +
                        "Device: " + device + "\n" +
                        "Android Version: " + androidVersion + "\n" +
                        "SDK/API Level: " + sdkVersion
        );


    }
    public static int getAppVersion(Context activity) {
        try {
            PackageInfo pinfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return pinfo.versionCode;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;

    }


}
