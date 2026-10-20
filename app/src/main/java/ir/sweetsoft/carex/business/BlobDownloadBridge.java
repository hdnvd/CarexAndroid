package ir.sweetsoft.carex.business;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class BlobDownloadBridge {
    private final Activity activity;
    private final String pendingBlobUrl="";

    public BlobDownloadBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void onBlobReady(String dataUri, String mimeType) {
        activity.runOnUiThread(() -> {
            if (dataUri == null || !dataUri.startsWith("data:")) {
                Toast.makeText(activity, "Failed to convert blob.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (PermissionHelper.hasStoragePermission(activity)) {
                DownloadManager.handleDataUriDownload(dataUri, mimeType, activity);
            } else {
//                requestStoragePermission(dataUri, mimeType);
            }
        });
    }

    @JavascriptInterface
    public void onBlobError(String error) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, "Blob fetch failed: " + error, Toast.LENGTH_SHORT).show()
        );
    }
}
