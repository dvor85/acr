package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
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
import android.os.Vibrator;
import android.text.format.DateFormat;

public class CallRecordService extends Service {

	private MyRecorder recorder = null;
	private PreferenceUtils sPref = null;
	private int lastReceivedState = -1;
	private int command;
	private String direct;
	private String phoneNumber;
	private ExecutorService es;
	private WaitForAnswer answerwait = null;
	private CountDownLatch startLatch;

	public static final int STATE_CALL_IO = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	public static final String EXTRA_CALL_DIRECTION = "direction";
	public static final String CALL_INCOMING = "in";
	public static final String CALL_OUTGOING = "out";

	public static final String CALLS_DIR = "calls";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startLatch = new CountDownLatch(2); // Для синхронизации двух переменных phoneNumber и direct
		recorder = new MyRecorder();
		es = Executors.newFixedThreadPool(3);
		sPref = PreferenceUtils.getInstance(this);
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		command = intent.getIntExtra(Utils.EXTRA_COMMAND, -1);
		if ((command != -1) && (lastReceivedState != command)) {
			lastReceivedState = command;
			es.execute(new RunService(intent, flags, startId, this));
		}

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

				switch (command) {

				case STATE_CALL_IO:
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					startLatch.countDown();
					direct = intent.getStringExtra(EXTRA_CALL_DIRECTION);
					startLatch.countDown();
					break;

				case STATE_CALL_START:
					startLatch.await(5, TimeUnit.SECONDS); // Ждать 5 секунд для установки переменных phoneNumber и direct
					if (CALL_OUTGOING.equals(direct) && Utils.checkRoot()) {
						answerwait = new WaitForAnswer();
						try {
							answerwait.join(Utils.SECOND * 120);
						} catch (InterruptedException ie) {
							Log.d(Utils.LOG_TAG, answerwait.getName() + " was interrupted!");
						} finally {
							if (answerwait != null) {
								answerwait.stopWait();
							}
						}

						if (command == STATE_CALL_START) {
							Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
							if ((v != null) && (v.hasVibrator())) {
								v.vibrate(sPref.getVibrate());
							}
						}
					}

					if ((command == STATE_CALL_START) && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED) && (!recorder.isStarted())) {

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

						recorder.startRecorder(MediaRecorder.AudioSource.VOICE_CALL, getFilename(), (int) Utils.HOUR * 6);
					}
					break;

				case STATE_CALL_END:
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

			if (recorder != null) {
				try {
					recorder.reset();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			stopSelf(startId);
		}

	}

	/**
	 * Класс который реализует паузу до ответа вызываемого абонента.
	 * 
	 * @author Dmitriy
	 */
	private class WaitForAnswer extends Thread {
		private Process ps = null;
		private Boolean running = false;
		private String ppid;

		public WaitForAnswer() {
			running = false;
			setName("WaitForAnswer");
			start();
		}

		@Override
		public void run() {
			BufferedReader stdout = null;
			BufferedWriter stdin = null;
			String line;
			try {
				synchronized (this) {
					if (!running) {
						ps = new ProcessBuilder("su").redirectErrorStream(true).start();
						ppid = ps.toString().substring(ps.toString().indexOf('=') + 1, ps.toString().indexOf(']'));
						stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
						stdin.append("logcat -c -b radio").append('\n');
						stdin.append("logcat -b radio").append('\n');
						stdin.close();
						stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
						running = true;
					}
				}
				while (((line = stdout.readLine()) != null) && (running)) {
					if (line.matches(".*ACTIVE.*")) {
						break;
					}
				}

			} catch (Exception e) {
			} finally {
				if (stdin != null) {
					try {
						stdin.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (stdout != null) {
					try {
						stdout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				stopWait();
			}
		}

		public synchronized void stopWait() {
			if (running) {
				running = false;
				try {
					// new Proc("su").killTree(ppid);
					Proc.processDestroy(ps);
				} catch (Exception e) {
				}
				Log.d(Utils.LOG_TAG, "Stop " + getName());
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
			if (answerwait != null) {
				answerwait.stopWait();
			}
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

		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");

	}

	/**
	 * Получить файл для записи
	 * 
	 * @return
	 * @throws IOException
	 */
	private File getFilename() throws IOException {
		File calls_dir = new File(sPref.getRootDir(), CALLS_DIR);

		String myDate = DateFormat.format("yyyy.MM.dd-kk_mm_ss", new Date()).toString();
		String phoneName = Utils.getContactName(this, phoneNumber);

		File dir = new File(calls_dir, phoneName + File.separator + phoneNumber);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String fn = direct + "_" + myDate + ".amr";

		return new File(dir, fn);
	}

}
