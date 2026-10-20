package ir.sweetsoft.carex.view.activities;

import android.content.Intent;
import android.os.Bundle;

import ir.sweetsoft.carex.R;
import ir.sweetsoft.carex.business.DeviceInfoHelper;


public class MainActivity extends WebAppMainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vw = findViewById(R.id.wv);
        retrybutton = findViewById(R.id.retrybutton);
        contactsupportbutton = findViewById(R.id.contactsupportbutton);
        ratingButton = findViewById(R.id.rateButton);
        rateLaterButton = findViewById(R.id.rateLaterButton);
        noInternetContainer = findViewById(R.id.nointernetcontainer);
        ratingContainer = findViewById(R.id.ratingContainer);
        splash = findViewById(R.id.splash);
        loading = findViewById(R.id.loading);
        this.initiateWebView();

        if (this.getIntent().getBooleanExtra("autoLoad", false)) {
            String url = getIntent().getStringExtra("url");
            var urlIsLocal = getIntent().getBooleanExtra("urlIsLocal", true);
            if (url != null)
                vw.loadUrl(urlIsLocal ? getSiteURL() + url : url);
        }
        DeviceInfoHelper.getDeviceInfo(this);
    }

    // The activity is singleTask, so deep links that arrive while it is open land here.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getData() != null)
            vw.loadUrl(getStartURL(intent));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(networkReceiver);
        } catch (Exception ignored) {}
    }
}
