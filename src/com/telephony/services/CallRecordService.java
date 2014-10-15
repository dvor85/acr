package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.format.DateFormat;

public class CallRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private String phoneNumber = null;
	private int command;
	private String direct = "";
	private File myFileName = null;
	private ExecutorService es;
	private RunWait runwait = null;

	public static final String CALLS_DIR = "calls";
	public static final String CALL_INCOMING = "in";
	public static final String CALL_OUTGOING = "out";

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
		command = intent.getIntExtra(Utils.EXTRA_COMMAND, -1);
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
				case Utils.STATE_IN_NUMBER:
					direct = CALL_INCOMING;
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					break;
				case Utils.STATE_OUT_NUMBER:
					direct = CALL_OUTGOING;
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					break;

				case Utils.STATE_CALL_START:
					if (CALL_OUTGOING.equals(direct) && Utils.checkRoot()) {
						runwait = new RunWait();
						runwait.run();
						if (command == Utils.STATE_CALL_START) {
							((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(sPref.getVibrate());
						}
					}

					if ((command == Utils.STATE_CALL_START) && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED) && (!recorder.started)) {
						myFileName = getFilename();
						recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
						recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
						recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
						recorder.setMaxDuration((int) (Utils.HOUR * 6));
						recorder.setOutputFile(myFileName.getAbsolutePath());

						OnErrorListener errorListener = new OnErrorListener() {
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

						recorder.prepare();
						recorder.start();
					}
					break;

				case Utils.STATE_CALL_END:
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
					recorder.reset();
					recorder.eraseFileIfLessThan(myFileName, 1024);
				}
				if (runwait != null) {
					runwait.stop();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {				
				stopSelf(startId);
			}
		}

	}

	/**
	 * Класс который реализует паузу до ответа вызываемого абонента
	 * Интерфейс Runnable реализуется для тестирования (он не обязателен)
	 * @author Dmitriy
	 *
	 */
	private class RunWait implements Runnable {
		private Process ps = null;
		private Boolean running = false;
		private String ppid;

		public RunWait() {
			running = false;
		}

		public void run() {
			BufferedReader stdout = null;
			BufferedWriter stdin = null;
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
				if (stdout != null) {
					stdout.close();
				}
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
					new Proc("su").killTree(ppid);
					Proc.processDestroy(ps);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.d(Utils.LOG_TAG, "Stop wait");
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			if (recorder != null) {
				recorder.release();
				recorder = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (runwait != null) {
			runwait.stop();
			runwait = null;
		}
		phoneNumber = null;
		sPref = null;
		es = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");

	}

	/**
	 * Получить файл для записи
	 * 
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	private File getFilename() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException,
			NoSuchAlgorithmException, NoSuchPaddingException {
		String calls_dir = sPref.getRootDir().getAbsolutePath() + File.separator + CALLS_DIR;

		File nomedia_file = new File(calls_dir, ".nomedia");
		if (!nomedia_file.exists()) {
			try {
				File root_dir = new File(calls_dir);
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

		File dir = new File(calls_dir, phoneName + File.separator + phoneNumber);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String fn = direct + "_" + myDate + ".amr";

		return new File(dir.getAbsolutePath(), fn);
	}

}
