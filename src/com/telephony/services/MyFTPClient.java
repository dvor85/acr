package com.telephony.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.util.Log;

public class MyFTPClient extends FTPClient {

	protected URL url;
	protected boolean isAuthorized = false;

	public MyFTPClient() {
		setControlEncoding("UTF8");
		setAutodetectUTF8(true);
		isAuthorized = false;
	}

	public void connect(String surl) throws SocketException, IOException, MalformedURLException {
		String username = "";
		String password = "";
		url = new URL(surl);
		String authority = url.getUserInfo();
		if (authority != null) {
			String[] auth = authority.split(":");
			username = auth[0];
			password = auth[1];
		}
		super.connect(url.getHost(), url.getPort());
		if (!FTPReply.isPositiveCompletion(getReplyCode())) {
			throw new IOException(getReplyString());
		}
		isAuthorized = super.login(username, password);
		if (!FTPReply.isPositiveCompletion(getReplyCode())) {
			throw new IOException(getReplyString());
		}
		setSoTimeout(5000);
		setControlKeepAliveTimeout(300); // set timeout to 5 minutes
		setFileType(BINARY_FILE_TYPE);
		enterLocalPassiveMode();
	}

	public void mkdirs(String dir) throws IOException {
		String[] dirs = dir.split(File.separator);
		try {
			for (String d : dirs) {
				if (!d.equals("")) {
					if (!changeWorkingDirectory(d)) {
						if (!makeDirectory(d)) {
							throw new IOException("Unable to create remote directory '" + d + "'.  error='" + getReplyString() + "'");
						}
						if (!super.changeWorkingDirectory(d)) {
							throw new IOException("Unable to change into newly created remote directory '" + d + "'.  error='" + getReplyString()
									+ "'");
						}
					}
				}
			}
		} finally {
			changeWorkingDirectory(File.separator);
		}
	}

	public void uploadFile(File root_dir, File file) throws IOException {
		FileInputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(file);
			String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), url.getPath());
			mkdirs(remote_dir);

			storeFile(remote_dir + File.separator + file.getName(), in);
			if (!FTPReply.isPositiveCompletion(getReplyCode())) {
				throw new IOException(getReplyString());
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}

		}
	}

	public File downloadFile(File root_dir, String remotefile) throws IOException {
		FileOutputStream out = null;
		InputStream in = null;
		File local_file = null;
		try {
			File local_dir = root_dir;
			File rf = new File(remotefile);
			if (rf.getParent() != null) {
				local_dir = new File(rf.getParent().replaceFirst(url.getPath(), root_dir.getAbsolutePath()));
			}

			if (!local_dir.exists()) {
				local_dir.mkdirs();
			}
			if (getFileSize(remotefile) > 0) {
				local_file = new File(local_dir.getAbsolutePath(), rf.getName());
				out = new FileOutputStream(local_file);
				retrieveFile(remotefile, out);
				if (!FTPReply.isPositiveCompletion(getReplyCode())) {
					throw new IOException(getReplyString());
				}
			}

			return local_file;
		} finally {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}

		}
	}

	private void show_status(String f, int c) {
		int reply = getReplyCode();
		Log.d(Utils.LOG_TAG, "isPositiveIntermediate " + f + " " + c + ": " + FTPReply.isPositiveIntermediate(reply) + " " + getReplyString());
		Log.d(Utils.LOG_TAG, "isPositiveCompletion " + f + " " + c + ": " + FTPReply.isPositiveCompletion(reply) + " " + getReplyString());
		Log.d(Utils.LOG_TAG, "isPositivePreliminary " + f + " " + c + ": " + FTPReply.isPositivePreliminary(reply) + " " + getReplyString());
		Log.d(Utils.LOG_TAG, "isProtectedReplyCode " + f + " " + c + ": " + FTPReply.isProtectedReplyCode(reply) + " " + getReplyString());
		Log.d(Utils.LOG_TAG, "isNegativePermanent " + f + " " + c + ": " + FTPReply.isNegativePermanent(reply) + " " + getReplyString());
		Log.d(Utils.LOG_TAG, "isNegativeTransient " + f + " " + c + ": " + FTPReply.isNegativeTransient(reply) + " " + getReplyString());

	}

	public String[] downloadFileText(String remotefile) throws IOException {
		byte[] buffer = new byte[1024];
		ArrayList<String> res = new ArrayList<String>();
		InputStream in = null;
		try {
			if (getFileSize(remotefile) > 0) {
				in = retrieveFileStream(remotefile);

				if (!FTPReply.isPositivePreliminary(getReplyCode())) {
					throw new IOException(getReplyString());
				}
				
				if (in != null) {
					int count = -1;
					while ((count = in.read(buffer)) > 0) {
						res.add(new String(buffer).substring(0, count));
					}
					in.close();
					if (!completePendingCommand()) {
						throw new IOException(getReplyString());
					}
				}
			}

			return res.toArray(new String[res.size()]);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public long getFileSize(String filePath) throws IOException {
		long fileSize = 0;
		FTPFile[] files = listFiles(filePath);
		if (files.length == 1 && files[0].isFile()) {
			fileSize = files[0].getSize();
		}
		return fileSize;
	}

	public File setHidden(File file) {
		File new_file = null;
		if ((file != null) && (file.exists()) && (!file.getName().startsWith("."))) {
			new_file = new File(file.getParent() + File.separator + "." + file.getName());
			file.renameTo(new_file);
		}
		return new_file;
	}

	@Override
	public void disconnect() throws IOException {
		super.disconnect();
		isAuthorized = false;
	}

}
