package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPReply;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

public class Updater {
	private MyFTPClient ftp = null;
	private WeakReference<Context> wContext = null;
	private Properties props = null;
	private PreferenceUtils sPref = null;

	public static final String VER_PROP_FILE = "ver.prop";
	public static final String REMOTE_VER = "ver";
	public static final String APK_REMOTE_FILE = "apk";

	public Updater(Context context, final MyFTPClient ftp) {
		this.ftp = ftp;
		wContext = new WeakReference<Context>(context);
		sPref = PreferenceUtils.getInstance(context);
		props = new Properties();
	}

	public boolean loadProps() throws IOException {
		InputStream in = null;
		try {
			in = ftp.retrieveFileStream(VER_PROP_FILE);
			if (!FTPReply.isPositivePreliminary(ftp.getReplyCode())) {
				throw new IOException(ftp.getReplyString());
			}
			if (in != null) {
				props.load(in);
				in.close();
				if (!ftp.completePendingCommand()) {
					throw new IOException(ftp.getReplyString());
				}
				return true;
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return false;
	}

	public int getRemoteVersion() {
		if (!props.isEmpty()) {
			return Integer.parseInt(props.getProperty(REMOTE_VER, "0"));
		}
		return 0;
	}

	public int getCurrentVersion() {
		int code = 0;
		PackageInfo pInfo = null;
		try {
			pInfo = wContext.get().getPackageManager().getPackageInfo(wContext.get().getPackageName(), 0);
			code = pInfo.versionCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return code;
	}

	public String getRemoteFile() {
		if (!props.isEmpty()) {

			return props.getProperty(APK_REMOTE_FILE, null);
		}
		return null;
	}

	public void updateAPK() {
		try {
			File apk_file = ftp.setHidden(ftp.downloadFile(sPref.getRootDir(), getRemoteFile()));
			if ((apk_file != null) && (apk_file.exists())) {
				if (Utils.CheckRoot()) {
					new Proc("su").exec(new String[] { "pm install " + apk_file.getAbsolutePath() });
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(apk_file), "application/vnd.android.package-archive");
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					if (wContext != null) {
						Utils.show_notification(wContext.get(), 0, intent);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void free() {
		wContext.clear();
		wContext = null;
		props.clear();
		props = null;
	}

}
