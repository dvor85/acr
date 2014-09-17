package com.telephony.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.FileLockInterruptionException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.Util;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.telephony.services.Utils.PreferenceUtils;

public class UploadService extends Service {

	private ExecutorService es;
	private PreferenceUtils sPref = null;
	private FTPClient ftp;
	private String server;
	private int port;
	private String username;
	private String password;
	private String root_dir;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();
		es = Executors.newFixedThreadPool(3);
		sPref = new PreferenceUtils(this);
		ftp = new FTPClient();
		ftp.setControlEncoding("UTF8");
		ftp.setAutodetectUTF8(true);

		sPref.setFtpServer("10.0.0.253");
		sPref.setFtpPort(21);
		sPref.setFtpUsername("upload");
		sPref.setFtpPasword("ghjuhtcc");
		server = sPref.getFtpServer();
		port = sPref.getFtpPort();
		username = sPref.getFtpUsername();
		password = sPref.getFtpPassword();

		root_dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + sPref.getRootCallsDir();

		Log.d(Utils.LogTag, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		es.execute(new RunService(intent, flags, startId));

		return super.onStartCommand(intent, flags, startId);

	}

	private class RunService implements Runnable {
		final Intent intent;
		final int flags;
		final int startId;

		public RunService(Intent intent, int flags, int startId) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
		}

		private void createDirectories(String dir) throws IOException {

			String[] dirs = dir.split("/");
			for (String d : dirs) {
				if (d.equals("")) {
					continue;
				}
				if (!ftp.changeWorkingDirectory(d)) {
					if (!ftp.makeDirectory(d)) {
						throw new IOException("Unable to create remote directory '" + d + "'.  error='" + ftp.getReplyString() + "'");
					}
					if (!ftp.changeWorkingDirectory(d)) {
						throw new IOException("Unable to change into newly created remote directory '" + d + "'.  error='" + ftp.getReplyString()
								+ "'");
					}
				}
			}
			ftp.changeWorkingDirectory("/");
		}

		public void run() {

			try {
				if (!server.equals("") && !(port == 0) && !username.equals("") && !password.equals("")) {
					ftp.connect(server, port);
					ftp.enterLocalPassiveMode();
					ftp.login(username, password);
					if (ftp.getReplyCode() == 230) {
						ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
						Log.d(Utils.LogTag, "charset " + ftp.getCharsetName() + " " + ftp.getStatus());
						ArrayList<File> list = Utils.rlistFiles(new File(root_dir), null);
						for (File file : list) {
							FileInputStream in = new FileInputStream(file);
							try {
								String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir, "");
								Log.d(Utils.LogTag, "try to upload " + remote_dir + File.separator + file.getName());
								createDirectories(remote_dir);

								if (!ftp.storeFile(remote_dir + File.separator + file.getName(), in)) {
									throw new IOException(ftp.getReplyString());
								}								
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								in.close();
							}
						}
					} else {
						throw new IOException(ftp.getReplyString());
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			stop();
		}

		public void stop() {
			try {
				ftp.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stopSelf();
		}

	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		es = null;
		sPref = null;
		ftp = null;
		Log.d(Utils.LogTag, getClass().getName() + " Destroy");
	}

}
