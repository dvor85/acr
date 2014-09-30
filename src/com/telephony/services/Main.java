package com.telephony.services;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		// Toast.makeText(this, getResources().getString(R.string.cant_run),
		// Toast.LENGTH_SHORT).show();

		// Intent myIntent = new Intent(this, ReglamentService.class);

		// startService(myIntent);

		// Log.d(Utils.LogTag, Utils.CheckRoot()+"");
		// AlarmManager am = (AlarmManager)
		// this.getSystemService(Context.ALARM_SERVICE);
		// Intent myIntent = new Intent(this, ReglamentService.class);
		// PendingIntent pmyIntent = PendingIntent.getService(this, 0, myIntent,
		// 0);
		// am.set(AlarmManager.ELAPSED_REALTIME, 50000L, pmyIntent);
		
		
		
		//Hide Main activity from launcher
//		Utils.setComponentState(this, Main.class, false);
		
		
		
//		Intent mIntent = new Intent(this, UploadService.class);
//		startService(mIntent);

//		try {
//			//new Proc("sh").exec(new String[] {"sleep 20"});
//			Log.d(Utils.LOG_TAG, new Proc("sh").getChilds("1967"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		finish();
	}

}
