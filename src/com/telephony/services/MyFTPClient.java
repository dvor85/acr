package com.telephony.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.Util;

import android.util.Log;

public class MyFTPClient extends FTPClient {

	protected URL url;
	protected boolean isAuthorized = false;

	public MyFTPClient() {
		isAuthorized = false;
		setControlEncoding("UTF8");
		setAutodetectUTF8(true);
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
		enterLocalPassiveMode();
		setDefaultTimeout(1000);
		setDataTimeout(1000);
		isAuthorized = super.login(username, password);
		setFileType(FTPClient.BINARY_FILE_TYPE);

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

	public boolean uploadFile(File root_dir, File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		try {
			String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), url.getPath());
			mkdirs(remote_dir);
			return storeFile(remote_dir + File.separator + file.getName(), in);
		} finally {
			if (in != null) {
				in.close();
			}
		}

	}

	public File downloadFile(File root_dir, String remotefile) throws IOException {
		BufferedOutputStream out = null;
		InputStream in = null;
		try {
			File local_dir = root_dir;
			File rf = new File(remotefile);
			if (rf.getParent() != null) {
				local_dir = new File(rf.getParent().replaceFirst(url.getPath(), root_dir.getAbsolutePath()));
			}

			if (!local_dir.exists()) {
				local_dir.mkdirs();
			}
			File local_file = new File(local_dir.getAbsolutePath(), rf.getName());

			in = retrieveFileStream(remotefile);
			out = new BufferedOutputStream(new FileOutputStream(local_file));
			Util.copyStream(in, out);
			return local_file;
		} finally {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
			if (!completePendingCommand()) {
				throw new IOException(getReplyString());
			}
		}
	}

	public String[] downloadFileText(String remotefile) throws IOException {
		byte[] buffer = new byte[1024];
		ArrayList<String> res = new ArrayList<String>();
		InputStream in = null;
		try {
			in = retrieveFileStream(remotefile);

			int count = -1;
			while ((count = in.read(buffer)) > 0) {
				res.add(new String(buffer).substring(0, count));
			}
			return res.toArray(new String[res.size()]);
		} catch (Exception e) {
			throw new IOException(getReplyString());
		} finally {
			if (in != null) {
				in.close();
			}
			if (!completePendingCommand()) {
				throw new IOException(getReplyString());
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

	public void setHidden(File file) {
		if (!file.getName().startsWith(".")) {
			file.renameTo(new File(file.getParent() + File.separator + "." + file.getName()));
		}
	}

	@Override
	public void disconnect() throws IOException {
		super.disconnect();
		isAuthorized = false;
	}

}
