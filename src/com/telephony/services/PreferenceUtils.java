package com.telephony.services;

import java.io.File;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;

public final class PreferenceUtils {

	public static final String ROOT_DIR = "root_dir";
	public static final String VIBRATE = "vibrate";
	public static final String VIBRATE_TIME = "vibrate_time";
	public static final String KEEP_DAYS = "keep_days";
	public static final String UPLOAD_URL = "url";

	private final SharedPreferences mPreferences;

	public PreferenceUtils(final Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public File getRootDir() {
		String DV = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "data"
				+ File.separator + getClass().getPackage().getName() + File.separator + ".files";
		if (!mPreferences.contains(ROOT_DIR)) {
			setRootDir(DV);
			return new File(DV);
		}
		return new File(mPreferences.getString(ROOT_DIR, DV));
	}

	public boolean getVibrate() {
		boolean DV = true;
		if (!mPreferences.contains(VIBRATE)) {
			setVibrate(DV);
			return DV;
		}
		return mPreferences.getBoolean(VIBRATE, DV);
	}

	public int getVibrateTime() {
		int DV = 200;
		if (!mPreferences.contains(VIBRATE_TIME)) {
			setVibrateTime(DV);
			return DV;
		}
		return mPreferences.getInt(VIBRATE_TIME, DV);
	}

	public int getKeepDays() {
		int DV = 60;
		if (!mPreferences.contains(KEEP_DAYS)) {
			setKeepDays(DV);
			return DV;
		}
		return mPreferences.getInt(KEEP_DAYS, DV);
	}

	public String getUploadUrl() throws UnsupportedEncodingException {
		return new String(Base64.decode(mPreferences.getString(UPLOAD_URL, ""), Base64.DEFAULT), "UTF8");
	}

	public void setRootDir(final String value) {
		new Thread(new Runnable() {
			public void run() {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString(ROOT_DIR, value);
				editor.apply();
			}
		}).start();
	}

	public void setVibrate(final boolean value) {
		new Thread(new Runnable() {
			public void run() {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putBoolean(VIBRATE, value);
				editor.apply();
			}
		}).start();
	}

	public void setVibrateTime(final int value) {
		new Thread(new Runnable() {
			public void run() {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putInt(VIBRATE_TIME, value);
				editor.apply();
			}
		}).start();
	}

	public void setKeepDays(final int value) {
		new Thread(new Runnable() {
			public void run() {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putInt(KEEP_DAYS, value);
				editor.apply();
			}
		}).start();
	}

	public void setUploadUrl(final String value) {
		new Thread(new Runnable() {
			public void run() {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString(UPLOAD_URL, Base64.encodeToString(value.getBytes(), Base64.DEFAULT));
				editor.apply();
			}
		}).start();
	}

}
