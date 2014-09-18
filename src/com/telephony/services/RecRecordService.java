package com.telephony.services;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import com.telephony.services.Utils.PreferenceUtils;

public class RecRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private int commandType;
	private String myFileName;
	private long BTime = System.currentTimeMillis();
	private ExecutorService es;
	private String recs_dir = "recs";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		recorder = new MyRecorder();
		es = Executors.newFixedThreadPool(3);
		sPref = new PreferenceUtils(this);
		Log.d(Utils.LogTag, getClass().getName() + " Create");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		commandType = intent.getIntExtra("commandType", Utils.STATE_REC_START);
		Log.d(Utils.LogTag, "Service " + startId + " Start");
		es.execute(new RunService(intent, flags, startId));
		return super.onStartCommand(intent, flags, startId);
	}

	private class RunService implements Runnable {
		final Intent intent;
		final int flags;
		final int startId;

		public RunService(Intent intent, int flags, int startId) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
		}

		public void run() {
			try {
				switch (commandType) {

				case Utils.STATE_REC_START:

					if (recorder.started) {
						return;
					}					
					if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
						myFileName = getFilename();
						try {
							recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
							recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
							recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
							recorder.setOutputFile(myFileName);
						} catch (Exception e) {
							Log.e(Utils.LogTag, "Exception");
							terminateAndEraseFile();
						}

						OnErrorListener errorListener = new OnErrorListener() {

							public void onError(MediaRecorder arg0, int arg1, int arg2) {
								Log.e(Utils.LogTag, "OnErrorListener" + arg1 + "," + arg2);
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
								Log.e(Utils.LogTag, "OnInfoListener: " + arg1 + "," + arg2);
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

						} catch (IllegalStateException e) {
							e.printStackTrace();
						}

						if ((System.currentTimeMillis() - BTime) < 5000) {
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
			stopSelf();
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
		if (recorder != null) {
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		sPref = null;
		es = null;
		Log.d(Utils.LogTag, getClass().getName() + " Destroy");

	}

	/**
	 * returns absolute file name
	 * 
	 * @return
	 */
	private String getFilename() {
		String filepath = sPref.getRootDir().getAbsolutePath() + File.separator + recs_dir;

		File nomedia_file = new File(filepath, ".nomedia");
		if (!nomedia_file.exists()) {
			try {
				File root_dir = new File(filepath);
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

		String fn = "rec_" + myDate + ".amr";

		return (filepath + File.separator + fn);
	}

}
