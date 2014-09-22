package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;

public class CallRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private String phoneNumber = null;
	private int commandType;
	private String direct = "";
	private String myFileName = null;
	private long BTime = System.currentTimeMillis();
	private ExecutorService es;
	private RunWait runwait = null;

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
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		commandType = intent.getIntExtra("commandType", Utils.STATE_IN_NUMBER);
		Log.d(Utils.LOG_TAG, "Service " + startId + " Start");
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
				case Utils.STATE_IN_NUMBER:
					direct = Utils.CALL_INCOMING;
					phoneNumber = intent.getStringExtra("phoneNumber");
					break;
				case Utils.STATE_OUT_NUMBER:
					direct = Utils.CALL_OUTGOING;
					phoneNumber = intent.getStringExtra("phoneNumber");
					break;

				case Utils.STATE_CALL_START:

					if (Utils.CALL_OUTGOING.equals(direct) && Utils.CheckRoot()) {
						runwait = new RunWait();
						runwait.run();
						if ((commandType == Utils.STATE_CALL_START) && (sPref.getVibrate())) {
							((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(sPref.getVibrateTime());
							Log.d(Utils.LOG_TAG, "VIBRATE");
						}
					}

					if ((commandType == Utils.STATE_CALL_START) && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED) && (!recorder.started)) {
						myFileName = getFilename();
						try {
							recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
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

				case Utils.STATE_CALL_END:
					try {
						try {
							if (recorder != null) {
								recorder.stop();
								recorder.reset();
								//recorder.release();
							}
							if (runwait != null) {
								runwait.stop();
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
			Log.d(Utils.LOG_TAG, "Stop service: " + startId);
			stopSelf(startId);
		}

	}

	private class RunWait implements Runnable {
		private Process ps = null;
		private Boolean running = false;
		private String ppid;

		public RunWait() {
			running = false;
		}

		public void run() {
			BufferedReader stdout;
			BufferedWriter stdin;
			String line;
			try {
				ps = new ProcessBuilder("su").redirectErrorStream(true).start();
				ppid = ps.toString().substring(ps.toString().indexOf('=') + 1, ps.toString().indexOf(']'));
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("logcat -c -b radio").append('\n');
				stdin.append("logcat -b radio").append('\n');
				stdin.flush();
				stdin.close();
				stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
				running = true;
				while (((line = stdout.readLine()) != null) && (running)) {
					if (line.matches(".*GET_CURRENT_CALLS.*(ACTIVE).*")) {
						break;
					}
				}
				stdout.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				new Thread(new Runnable() {
					public void run() {
						stop();
					}
				}).start();

			}
		}

		void stop() {
			if (running) {
				running = false;
				try {
					// Log.d(Utils.LOG_TAG, "BEGIN");
					// Log.d(Utils.LOG_TAG, "Childs of " + ppid + ": " + new
					// Proc("su").getChilds("398"));

					// Thread.currentThread();
					new Proc("su").killTree(ppid);
					//TimeUnit.SECONDS.sleep(10);
					Proc.processDestroy(ps);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.d(Utils.LOG_TAG, "Stop wait");
			}
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
				//recorder.release();			
			}
			if (runwait != null) {
				runwait.stop();
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
		phoneNumber = null;
		runwait = null;
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
		String filepath = sPref.getRootDir().getAbsolutePath() + File.separator + Utils.CALLS_DIR;

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

		String phoneName = Utils.getContactName(this, phoneNumber);

		File file = new File(filepath, phoneName + File.separator + phoneNumber);

		if (!file.exists()) {
			file.mkdirs();
		}
		String fn = direct + "_" + myDate + ".amr";

		return (file.getAbsolutePath() + File.separator + fn);
	}

}
