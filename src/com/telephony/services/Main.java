package com.telephony.services;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class Main extends Activity {
	private static final String LogTag = "myLogs";
	// private Utils utils = new Utils();
	String s;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		Toast.makeText(this, getResources().getString(R.string.cant_run), Toast.LENGTH_SHORT).show();
		// finish();

		// new Thread(new RunnableCmd("")).start();

		/*
		 * RunnableCmd c = new RunnableCmd(""); try { Log.d(LogTag,
		 * c.CheckRoot() + ""); } catch (IOException e) { e.printStackTrace(); }
		 */
		// Log.d(LogTag, ""+(s=null));
		// new RunWait().run();

		finish();
	}

	/*
	 * public class RunWait implements Runnable { private Process ps = null;
	 * private Boolean running = false; private String ppid;
	 * 
	 * public RunWait() { running = false; }
	 * 
	 * public void run() { BufferedReader stdout; BufferedWriter stdin; String
	 * line; try { ps = new
	 * ProcessBuilder("su").redirectErrorStream(true).start(); ppid =
	 * ps.toString().substring(ps.toString().indexOf('=') + 1,
	 * ps.toString().indexOf(']')); stdin = new BufferedWriter(new
	 * OutputStreamWriter(ps.getOutputStream()));
	 * stdin.append("logcat -c -b radio").append('\n');
	 * stdin.append("logcat -b radio").append('\n'); stdin.flush();
	 * stdin.close(); stdout = new BufferedReader(new
	 * InputStreamReader(ps.getInputStream())); running = true; while (((line =
	 * stdout.readLine()) != null) && (running)) { if
	 * (line.matches(".*GET_CURRENT_CALLS.*(ACTIVE).*")) { break; } }
	 * 
	 * new Thread(new Runnable() { public void run() { stop(); } }).start(); }
	 * catch (Exception e) { e.printStackTrace(); } finally { if (!running) {
	 * ps.destroy(); } } }
	 * 
	 * void stop() {
	 * 
	 * if (running) { new Utils.KillProc("su").killTree(ppid); ps.destroy();
	 * //if (commandType == STATE_CALL_START) { // ((Vibrator)
	 * getSystemService(VIBRATOR_SERVICE)).vibrate(200); // Log.d(LogTag,
	 * "VIBRATE"); //} running = false; Log.d(LogTag, "Stop wait"); }
	 * 
	 * } }
	 */

}
