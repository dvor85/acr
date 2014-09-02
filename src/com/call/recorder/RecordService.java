/*
 *  Copyright 2012 Kobi Krasnoff
 * 
 * This file is part of Call recorder For Android.

    Call recorder For Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Call recorder For Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Call recorder For Android.  If not, see <http://www.gnu.org/licenses/>
 */
package com.call.recorder;

import java.io.File;
import java.io.IOException;
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
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.format.DateFormat;
import android.util.Log;

public class RecordService extends Service {

	public static final String LISTEN_ENABLED = "ListenEnabled";
	public static final String FILE_DIRECTORY = ".calls";

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	private MediaRecorder recorder = null;
	private String phoneNumber = null;
	private int commandType;
	private String direct;

	private String myFileName;
	private static final String LogTag = "CallRecorder";

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
				Log.e("Call recorder IllegalStateException: ", "");
				terminateAndEraseFile();
			} catch (Exception e) {
				Log.e("Call recorder Exception: ", "");
				terminateAndEraseFile();
			}

			OnErrorListener errorListener = new OnErrorListener() {

				public void onError(MediaRecorder arg0, int arg1, int arg2) {
					Log.e("Call recorder OnErrorListener: ", arg1 + "," + arg2);
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
					Log.e("Call recorder OnInfoListener: ", arg1 + "," + arg2);
					arg0.stop();
					arg0.reset();
					arg0.release();
					arg0 = null;
					terminateAndEraseFile();
				}

			};
			recorder.setOnInfoListener(infoListener);

			try {
				recorder.prepare();
				recorder.start();

			} catch (IllegalStateException e) {
				Log.e("Call recorder IllegalStateException: ", "");
				terminateAndEraseFile();
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("Call recorder IOException: ", "");
				terminateAndEraseFile();
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Call recorder Exception: ", "");
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
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
			stopForeground(true);
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
	 * Obtains the contact list for the currently selected account.
	 * 
	 * @return A cursor for for accessing the contact list.
	 */
	private String getContactName(String phoneNum) {
		String res = phoneNum;
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNum));
		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
		Cursor names = getContentResolver().query(uri, projection, null, null, null);
		try {
			int indexName = names.getColumnIndex(PhoneLookup.DISPLAY_NAME);
			if (names.getCount() > 0) {
				names.moveToFirst();
				do {
					String name = names.getString(indexName);
					res = name;
				} while (names.moveToNext());
			}
		} finally {
			names.close();
		}
		return res;
	}

	/**
	 * returns absolute file directory
	 * 
	 * @return
	 */
	private String getFilename() {
		String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();

		String myDate = new String();
		myDate = (String) DateFormat.format("dd.MM.yyyy-kk_mm_ss", new Date());

		String phoneName = getContactName(phoneNumber);

		File file = new File(filepath, FILE_DIRECTORY + File.separator + phoneName + File.separator + phoneNumber);

		if (!file.exists()) {
			file.mkdirs();
		}
		String fn = direct + "_" + myDate + ".amr";

		return (file.getAbsolutePath() + File.separator + fn);
	}

}
