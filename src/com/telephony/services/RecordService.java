package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileLockInterruptionException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;

public class RecordService extends Service {

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	private static final String LogTag = "myLogs";

	private static MediaRecorder recorder = null;
	private static String phoneNumber = null;
	private static int commandType;
	private static String direct = "";
	private static String myFileName;
	private static long BTime = System.currentTimeMillis();
	private static ExecutorService es;
	private static RunWait runwait = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		es = Executors.newFixedThreadPool(3);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		commandType = intent.getIntExtra("commandType", STATE_IN_NUMBER);
		Log.d(LogTag, "Command type:" + commandType);

		switch (commandType) {
		case STATE_IN_NUMBER:
			direct = "in";
			if (phoneNumber == null)
				phoneNumber = intent.getStringExtra("phoneNumber");
			break;
		case STATE_OUT_NUMBER:
			direct = "out";
			if (phoneNumber == null)
				phoneNumber = intent.getStringExtra("phoneNumber");
			break;

		case STATE_CALL_START:
			es.execute(new Runnable() {
				public void run() {
					myFileName = getFilename();
					try {
						recorder = new MediaRecorder();
						recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
						recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
						recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
						recorder.setOutputFile(myFileName);
					} catch (IllegalStateException e) {
						Log.e(LogTag, "IllegalStateException");
						terminateAndEraseFile();
					} catch (Exception e) {
						Log.e(LogTag, "Exception");
						terminateAndEraseFile();
					}

					OnErrorListener errorListener = new OnErrorListener() {

						public void onError(MediaRecorder arg0, int arg1, int arg2) {
							Log.e(LogTag, "OnErrorListener" + arg1 + "," + arg2);
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
							Log.e(LogTag, "OnInfoListener: " + arg1 + "," + arg2);
							arg0.stop();
							arg0.reset();
							arg0.release();
							arg0 = null;
							terminateAndEraseFile();
						}

					};
					recorder.setOnInfoListener(infoListener);

					try {
						if (direct.equals("out")) {
							runwait = new RunWait();
							runwait.run();
						}
						if (commandType == STATE_CALL_START) {
							BTime = System.currentTimeMillis();
							recorder.prepare();
							recorder.start();
						}

					} catch (IllegalStateException e) {
						Log.e(LogTag, "IllegalStateException");
						terminateAndEraseFile();
						e.printStackTrace();
					} catch (IOException e) {
						Log.e(LogTag, "IOException");
						terminateAndEraseFile();
						e.printStackTrace();
					} catch (Exception e) {
						Log.e(LogTag, "Exception");
						terminateAndEraseFile();
						e.printStackTrace();
					}

				}
			});

			break;
		case STATE_CALL_END:
			try {
				if (runwait != null) {
					runwait.stop();
					runwait = null;
				}
				if (recorder != null) {
					recorder.stop();
				}

			} catch (IllegalStateException e) {
				e.printStackTrace();
			} finally {
				if (recorder != null) {
					recorder.reset();
					recorder.release();
					recorder = null;
					phoneNumber = null;
				}
			}

			if ((System.currentTimeMillis() - BTime) < 5000) {
				terminateAndEraseFile();
			}
			stopSelf();
			break;
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public class RunWait implements Runnable {
		private Process ps = null;
		private String pr;
		private Boolean running = false;

		public RunWait() {
		}

		public void run() {
			BufferedReader stdout;
			BufferedWriter stdin;
			String line;
			try {
				ps = new ProcessBuilder("su").redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("logcat -c -b radio").append('\n');
				stdin.append("logcat -b radio").append('\n');
				stdin.flush();
				stdin.close();

				stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
				Log.d(LogTag, ps.toString());
				running = true;
				while (((line = stdout.readLine()) != null) && (running)) {
					if (line.matches(".*GET_CURRENT_CALLS.*(ACTIVE).*")) {
						break;
					}
				}
				stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		void stop() {
			if (running) {
				pr = ps.toString();
				Utils.killproc(pr.substring(pr.indexOf('=') + 1, pr.indexOf(']')));
				ps.destroy();
				Log.d(LogTag, "End wait");
				if (commandType == STATE_CALL_START) {
					Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					v.vibrate(200);
					Log.d(LogTag, "VIBRATE");
				}
				running = false;
			}
		}
	}

	/**
	 * in case it is impossible to record
	 */
	private void terminateAndEraseFile() {
		try {
			if (runwait != null) {
				runwait.stop();
				runwait = null;
			}
			if (recorder != null) {
				recorder.stop();
			}

		} catch (IllegalStateException e) {
			e.printStackTrace();
		} finally {
			if (recorder != null) {
				recorder.reset();
				recorder.release();
				recorder = null;
				phoneNumber = null;
			}
		}
		File file = new File(myFileName);

		if (file.exists()) {
			file.delete();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		recorder = null;
		phoneNumber = null;
		runwait = null;
		es = null;
		Log.d(LogTag, "Destroy");

	}

	/**
	 * returns absolute file name
	 * 
	 * @return
	 */
	private String getFilename() {
		String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getResources().getString(R.string.calls_dir);

		File nomedia_file = new File(filepath, ".nomedia");
		if (!nomedia_file.exists()) {
			try {
				File root_dir = new File(filepath);
				if (!root_dir.exists()) {
					root_dir.mkdirs();
				}
				nomedia_file.createNewFile();
			} catch (IOException e) {
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
