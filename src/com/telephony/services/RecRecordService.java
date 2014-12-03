package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.text.format.DateFormat;

public class RecRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private int command;
	private ExecutorService es;
	private int max_duration;

	public static final int COMMAND_REC_START = 1;
	public static final int COMMAND_REC_STOP = 2;

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
		command = intent.getIntExtra(Utils.EXTRA_COMMAND, 0);
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

		@Override
		public void run() {
			try {
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
				max_duration = intent.getIntExtra(Utils.EXTRA_DURATION, (int) Utils.HOUR * 6);
				switch (command) {
				case COMMAND_REC_START:

					if ((Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED) && (!recorder.isStarted())) {

						OnErrorListener errorListener = new OnErrorListener() {
							@Override
							public void onError(MediaRecorder mr, int what, int extra) {
								switch (what) {
								case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
									Log.d(Utils.LOG_TAG,
											context.getClass().getName()
													+ ": Media server died. In this case, the application must release the MediaRecorder object and instantiate a new one.");
									break;
								default:
									Log.d(Utils.LOG_TAG, context.getClass().getName() + ": Unspecified media recorder error.");
									break;
								}
								stop();
							}
						};
						recorder.setOnErrorListener(errorListener);

						OnInfoListener infoListener = new OnInfoListener() {
							@Override
							public void onInfo(MediaRecorder mr, int what, int extra) {
								switch (what) {
								case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
									Log.d(Utils.LOG_TAG, context.getClass().getName()
											+ ": A maximum duration had been setup and has now been reached.");
									break;
								case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
									Log.d(Utils.LOG_TAG, context.getClass().getName()
											+ ": A maximum filesize had been setup and has now been reached.");
									break;
								default:
									Log.d(Utils.LOG_TAG, context.getClass().getName() + ": Unspecified media recorder error.");
									break;
								}
								stop();
							}
						};
						recorder.setOnInfoListener(infoListener);

						recorder.startRecorder(MediaRecorder.AudioSource.VOICE_RECOGNITION, getFilename(), max_duration);

					}
					break;

				case COMMAND_REC_STOP:
					stop();
					break;
				default:
					stop();
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				stop();
			}

		}

		public void stop() {
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			try {
				if (recorder != null) {
					try {
						recorder.reset();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				stopSelf(startId);
			}
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		es.shutdown();
		if (recorder != null) {
			try {
				recorder.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			if ((es.isShutdown()) && (!es.awaitTermination(5, TimeUnit.SECONDS))) {
				es.shutdownNow();
				if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
					Log.d(Utils.LOG_TAG, "Pool did not terminated");
				}
			}
		} catch (InterruptedException ie) {
			es.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
		sPref = null;
		es = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");

	}

	/**
	 * Получить файл для записи
	 * 
	 * @return
	 * @throws IOException
	 */
	private File getFilename() throws IOException {
		File recs_dir = new File(sPref.getRootDir(), RECS_DIR);
		if (!recs_dir.exists()) {
			recs_dir.mkdirs();
		}

		String myDate = DateFormat.format("yyyy.MM.dd-kk_mm_ss", new Date()).toString();
		String fn = MIC_RECORD + "_" + myDate + ".amr";

		return new File(recs_dir, fn);
	}

}
