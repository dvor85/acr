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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

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
		isAuthorized = super.login(username, password);
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
			setFileType(FTPClient.BINARY_FILE_TYPE);
			String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), url.getPath());
			mkdirs(remote_dir);
			return storeFile(remote_dir + File.separator + file.getName(), in);
		} finally {
			if (in != null) {
				in.close();
			}
		}

	}

	public boolean downloadFile(File root_dir, String remotefile) throws IOException {
		BufferedOutputStream out = null;
		try {
			File local_dir = root_dir;
			setFileType(FTPClient.BINARY_FILE_TYPE);
			File rf = new File(remotefile);
			if (rf.getParent() != null) {
				local_dir = new File(rf.getParent().replaceFirst(url.getPath(), root_dir.getAbsolutePath()));
			}

			if (!local_dir.exists()) {
				local_dir.mkdirs();
			}
			out = new BufferedOutputStream(new FileOutputStream(new File(local_dir.getAbsolutePath(), rf.getName())));
			return retrieveFile(remotefile, out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public String downloadFileText(String remotefile) throws IOException {
		byte[] buffer = new byte[(int) getFileSize(remotefile)];
		StringBuilder res = new StringBuilder();
		InputStream in = null;		
		try {
			setFileType(FTPClient.BINARY_FILE_TYPE);
			in = retrieveFileStream(remotefile);
			while (in.read(buffer) > 0) {
				res.append(new String(buffer));
			}
			return res.toString();
		} catch (Exception e) {
			throw new IOException(getReplyString());
		}
		finally {
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
