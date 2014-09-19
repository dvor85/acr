package com.telephony.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract.PhoneLookup;

public class Utils {

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;
	public static final int STATE_REC_START = 4;
	public static final int STATE_REC_STOP = 5;

	public static final String CALL_INCOMING = "in";
	public static final String CALL_OUTGOING = "out";
	public static final String MIC_RECORD = "rec";

	public static final int MEDIA_MOUNTED = 0;
	public static final int MEDIA_MOUNTED_READ_ONLY = 1;
	public static final int NO_MEDIA = 2;

	public static final String LOG_TAG = "myLogs";

	/**
	 * Check if app have root
	 * 
	 * @return
	 */
	public static Boolean CheckRoot() {
		BufferedWriter stdin;
		Process ps = null;
		try {
			ps = new ProcessBuilder("su").start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			stdin.append("exit").append('\n');
			stdin.flush();
			stdin.close();
			ps.waitFor();
			return ps.exitValue() == 0;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ps != null) {
				ps.destroy();
			}
		}

		return false;
	}

	/**
	 * checks if an external memory card is available
	 * 
	 * @return
	 */
	public static int updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return MEDIA_MOUNTED;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return MEDIA_MOUNTED_READ_ONLY;
		} else {
			return NO_MEDIA;
		}

	}

	public static ArrayList<File> rlistFiles(File root, FilenameFilter filter) {
		ArrayList<File> sb = new ArrayList<File>();
		File[] list = root.listFiles(filter);
		if (list != null) {
			for (File f : list) {
				if (f.isDirectory()) {
					sb.addAll(rlistFiles(f, filter));
				} else {
					sb.add(f);
				}
			}
		}
		return sb;
	}

	/**
	 * Wrapper for setComponentEnabledSetting
	 * 
	 * @param context
	 * @param cls
	 *            - class to change status
	 * @param status
	 */
	public static void setComponentState(Context context, Class<?> cls, boolean status) {
		int pmState;
		try {
			ComponentName component = new ComponentName(context, cls);
			if (status) {
				pmState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
			} else {
				pmState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
			}
			// context.getPackageManager().setComponentEnabledSetting(component,
			// pmState, PackageManager.DONT_KILL_APP);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtains the contact list for the currently selected account.
	 * 
	 * @return A cursor for for accessing the contact list.
	 */
	public static String getContactName(Context context, String phoneNum) {
		String res = phoneNum;
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNum));
		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };

		Cursor names = context.getContentResolver().query(uri, projection, null, null, null);
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
			names = null;
		}
		return res;
	}

}
