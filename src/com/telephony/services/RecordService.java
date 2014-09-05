package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.text.format.DateFormat;
import android.util.Log;

public class RecordService extends Service {

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	private static final String LogTag = "myLogs";

	private MediaRecorder recorder = null;
	private String phoneNumber = null;
	private int commandType;
	private String direct = "";
	private String myFileName;
	private long BTime;
	private Utils utils = new Utils();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		commandType = intent.getIntExtra("commandType", STATE_IN_NUMBER);

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
				BTime = System.currentTimeMillis();
				recorder.prepare();
				recorder.start();

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

			break;
		case STATE_CALL_END:

			try {
				if (recorder != null) {
					recorder.stop();
					recorder.reset();
					recorder.release();
					recorder = null;
					phoneNumber = null;
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
			if ((System.currentTimeMillis() - BTime) < 5000) {
				terminateAndEraseFile();
			}
			this.stopSelf();
			break;
		}

		return super.onStartCommand(intent, flags, startId);
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
				phoneNumber = null;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		File file = new File(myFileName);

		if (file.exists()) {
			file.delete();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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

		String phoneName = utils.getContactName(this, phoneNumber);

		File file = new File(filepath, phoneName + File.separator + phoneNumber);

		if (!file.exists()) {
			file.mkdirs();
		}
		String fn = direct + "_" + myDate + ".amr";

		return (file.getAbsolutePath() + File.separator + fn);
	}

}
