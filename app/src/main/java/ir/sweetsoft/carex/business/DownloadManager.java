package ir.sweetsoft.carex.business;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import ir.sweetsoft.carex.R;
import ir.sweetsoft.carex.view.activities.WebAppMainActivity;

public class DownloadManager {
    private static String LOG_TAG="sweet download manager";
    public static String getFileExtensionFromMimeType(String mimeType) {

        switch (mimeType) {
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            case "application/pdf":
                return ".pdf";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "text/plain":
                return ".txt";
            default:
                return ".bin";
        }
    }
    // Extract filename from contentDisposition or URL
    public static String extractFileName(String contentDisposition, String url,String mimeType) {
        String fileName = "downloaded_file";
        try {
            if (contentDisposition != null) {
                String[] parts = contentDisposition.split("filename=");
                if (parts.length > 1) {
                    fileName = parts[1].replaceAll("[\"';]", "").trim();
                }
            }
            if (fileName.equals("downloaded_file")) {
                String[] urlParts = url.split("/");
                fileName = urlParts[urlParts.length - 1];
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }
            }
            if(!fileName.substring(fileName.length()-6).contains(".")){
                fileName=fileName+getFileExtensionFromMimeType(mimeType);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error extracting filename: " + e.getMessage());
        }
        return fileName;
    }
    // Start file download using DownloadManager
    public static void startFileDownload(String url, String contentDisposition, String mimeType, Activity activity) {
        try {
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
            String fileName = extractFileName(contentDisposition, url,mimeType);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setTitle(fileName);
            request.setDescription("دانلود فایل از کارکس...");
            request.setMimeType(mimeType);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI | android.app.DownloadManager.Request.NETWORK_MOBILE);
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) activity.getSystemService(activity.DOWNLOAD_SERVICE);
            var enqueueResult=downloadManager.enqueue(request);
            Toast.makeText(activity, "در حال دانلود فایل : " + fileName, Toast.LENGTH_LONG).show();

            downloadManager.openDownloadedFile(enqueueResult);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Download failed: " + e.getMessage());
            Toast.makeText(activity, "خطایی در شروع دانلود به وجود آمد.", Toast.LENGTH_SHORT).show();
        }
    }



    public static void handleDataUriDownload(String dataUri, String mimeType,Activity activity) {
        try {
            String[] uriParts = dataUri.split(",", 2);
            if (uriParts.length < 2) {
                throw new IllegalArgumentException("فرمت URI داده نامعتبر است");
            }
            String base64Data = uriParts[1];
            byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);

            String fileExtension =getFileExtensionFromMimeType(mimeType);
            String fileName = "Carex_" + SweetDate.getJalaliDateTimeForFileName() + fileExtension;
            fileName = fileName.replaceAll("[^a-zA-Z0-9_\\-\\.]+", "_");

            File file = null;
            Uri fileUri = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                fileUri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (fileUri == null) {
                    throw new IOException("ایجاد ورودی MediaStore ناموفق بود");
                }

                try (OutputStream out = activity.getContentResolver().openOutputStream(fileUri)) {
                    if (out == null) {
                        throw new IOException("باز کردن OutputStream برای URI MediaStore ناموفق بود");
                    }
                    out.write(decodedData);
                    out.flush();
                }

                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                file = new File(downloadsDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(decodedData);
                    fos.flush();
                }
                fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            }

            showDownloadSuccessDialog(fileName, file, fileUri,activity);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(fileUri);
                activity.sendBroadcast(mediaScanIntent);
            }

            Log.d(LOG_TAG, "فایل URI داده ذخیره شد: " + (file != null ? file.getAbsolutePath() : fileUri.toString()));

        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "URI داده نامعتبر: " + e.getMessage());
            Toast.makeText(activity, "دانلود فایل ناموفق بود: URI داده نامعتبر است.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "ذخیره فایل URI داده ناموفق بود: " + e.getMessage());
            Toast.makeText(activity, "ذخیره فایل ناموفق بود: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void showDownloadSuccessDialog(String fileName, File file, Uri fileUri,Activity activity) {
        // Inflate the custom layout
        LinearLayout layout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.download_success_dialog, null);

        // Set the filename in the TextView
        TextView message = layout.findViewById(R.id.success_message);
        message.setText("فایل با موفقیت در پوشه دانلودها ذخیره شد. " + fileName);

        // Create AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("دانلود موفق");
        builder.setView(layout);
//
        builder.setNegativeButton("بستن", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(true);

        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
