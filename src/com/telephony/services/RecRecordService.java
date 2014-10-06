package com.telephony.services;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

public class RecRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private int command;
	private String myFileName = null;
	private long BTime = System.currentTimeMillis();
	private ExecutorService es;

	public static final String RECS_DIR = "recs";
	public static final String MIC_RECORD = "rec";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		recorder = new MyRecorder();
		es = Executors.newFixedThreadPool(3);
		sPref = PreferenceUtils.getInstance(this);
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		command = intent.getIntExtra(Utils.EXTRA_COMMAND, Utils.STATE_REC_START);
		Log.d(Utils.LOG_TAG, "Service " + startId + " Start");
		es.execute(new RunService(intent, flags, startId, this));
		return START_REDELIVER_INTENT;
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
			try {
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
				switch (command) {
				case Utils.STATE_REC_START:

					if ((Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED) && (!recorder.started)) {
						myFileName = getFilename();
						try {
							recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
							recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
							recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
							recorder.setOutputFile(myFileName);
						} catch (Exception e) {
							terminateAndEraseFile();						
								e.printStackTrace();				
						}

						OnErrorListener errorListener = new OnErrorListener() {

							public void onError(MediaRecorder arg0, int arg1, int arg2) {
								Log.e(Utils.LOG_TAG, "OnErrorListener" + arg1 + "," + arg2);
								arg0.stop();
								arg0.reset();
								arg0.release();
								arg0 = null;
								terminateAndEraseFile();
							}

						};
						recorder.setOnErrorListener(errorListener);
						OnInfoListener infoListener = new OnInfoListener() {

							public void onInfo(MediaRecorder arg0, int arg1, int arg2) {
								Log.e(Utils.LOG_TAG, "OnInfoListener: " + arg1 + "," + arg2);
								arg0.stop();
								arg0.reset();
								arg0.release();
								arg0 = null;
								terminateAndEraseFile();
							}

						};
						recorder.setOnInfoListener(infoListener);

						try {
							BTime = System.currentTimeMillis();
							recorder.prepare();
							recorder.start();

						} catch (Exception e) {
							terminateAndEraseFile();					
							e.printStackTrace();
						}
					}
					break;

				case Utils.STATE_REC_STOP:
					try {
						try {
							if (recorder != null) {
								recorder.stop();
								recorder.reset();
								recorder.release();
								recorder = null;
							}

						} catch (Exception e) {					
								e.printStackTrace();							
						}

						if ((System.currentTimeMillis() - BTime) < Utils.SECOND * 5) {
							terminateAndEraseFile();
						}
					} finally {
						stop();
					}
					break;
				}
			} catch (Exception e) {			
					e.printStackTrace();				
			}

		}

		public void stop() {
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			stopSelf(startId);
		}

	}

	/**
	 * in case it is impossible to record
	 */
	private void terminateAndEraseFile() {
		try {
			if (recorder != null) {
				recorder.stop();
				recorder.reset();
				recorder.release();
				recorder = null;
			}

		} catch (Exception e) {			
				e.printStackTrace();			
		}
		if (myFileName != null) {
			File file = new File(myFileName);

			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			if (recorder != null) {
				recorder.reset();
				recorder.release();
				recorder = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sPref = null;
		es = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");

	}

	/**
	 * returns absolute file name
	 * 
	 * @return
	 */
	private String getFilename() {
		String recs_dir = sPref.getRootDir().getAbsolutePath() + File.separator + RECS_DIR;

		File nomedia_file = new File(recs_dir, ".nomedia");
		if (!nomedia_file.exists()) {
			try {
				File root_dir = new File(recs_dir);
				if (!root_dir.exists()) {
					root_dir.mkdirs();
				}
				nomedia_file.createNewFile();
			} catch (Exception e) {				
					e.printStackTrace();
			}
		}

		String myDate = new String();
		myDate = DateFormat.format("yyyy.MM.dd-kk_mm_ss", new Date()).toString();

		String fn = MIC_RECORD + "_" + myDate + ".amr";

		return (recs_dir + File.separator + fn);
	}

}
