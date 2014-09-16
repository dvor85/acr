package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
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

	public static final String LogTag = "myLogs";

	/**
	 * Check if app have root
	 * 
	 * @return
	 */
	public static Boolean CheckRoot() {
		BufferedWriter stdin;
		Process ps = null;
		Boolean res = false;
		try {
			ps = new ProcessBuilder("su").start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			stdin.append("exit").append('\n');
			stdin.flush();
			stdin.close();
			ps.waitFor();
			res = ps.exitValue() == 0;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
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
			context.getPackageManager().setComponentEnabledSetting(component, pmState, PackageManager.DONT_KILL_APP);
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

	/**
	 * Class for kill pid using shell
	 * 
	 * @author Dmitriy
	 *
	 */
	public static class KillProc {
		private String shell;

		/**
		 * @param shell
		 *            - sh or su
		 */
		public KillProc(String shell) {
			this.shell = shell;
		}

		/**
		 * Recursive search pid with ppid as parent
		 * 
		 * @param ppid
		 *            - parent pid
		 * @return pids with space as separator
		 */
		public String getChilds(String ppid) {
			// рекурсивный поиск всех дочерних процессов
			BufferedReader stdout;
			BufferedWriter stdin;
			Process ps;
			String[] psinfo;
			StringBuilder sb = new StringBuilder();

			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("toolbox ps").append('\n');
				stdin.flush();
				stdin.close();
				stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
				String line = stdout.readLine();
				while (line != null) {
					psinfo = line.split(" +");
					if (psinfo[2].equals(ppid)) {
						sb.append(psinfo[1]).append(' ');
						sb.append(getChilds(psinfo[1]));
					}
					line = stdout.readLine();
				}
				stdout.close();
				ps.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return sb.toString();
		}

		/**
		 * Kill tree of pids
		 * 
		 * @param ppid
		 *            - parent pid
		 */
		public void killTree(String ppid) {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;
			String cpid;
			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				cpid = getChilds(ppid);
				stdin.append("kill -9 " + ppid + " " + cpid).append('\n');
				stdin.flush();
				stdin.close();
				ps.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

		/**
		 * Kill one proc by pid
		 * 
		 * @param pid
		 */
		public void kill(String pid) {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;
			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("kill -9 " + pid).append('\n');
				stdin.flush();
				stdin.close();
				ps.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}

	public static final class PreferenceUtils {

		public static final String ROOT_CALLS_DIR = "calls_dir";
		public static final String ROOT_RECS_DIR = "recs_dir";
		public static final String VIBRATE = "vibrate";
		public static final String VIBRATE_TIME = "vibrate_time";
		public static final String KEEP_DAYS = "keep_days";

		private final SharedPreferences mPreferences;

		public PreferenceUtils(final Context context) {
			mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		}

		public String getRootCallsDir() {
			String DV = ".calls";
			if (!mPreferences.contains(ROOT_CALLS_DIR)) {
				setRootCallsDir(DV);
				return DV;
			}
			return mPreferences.getString(ROOT_CALLS_DIR, DV);
		}

		public String getRootRecsDir() {
			String DV = ".recs";
			if (!mPreferences.contains(ROOT_RECS_DIR)) {
				setRootCallsDir(DV);
				return DV;
			}
			return mPreferences.getString(ROOT_CALLS_DIR, DV);
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

		public void setRootCallsDir(final String value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putString(ROOT_CALLS_DIR, value);
					editor.apply();
				}
			}).start();
		}

		public void setRootRecsDir(final String value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putString(ROOT_RECS_DIR, value);
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

	}
}
