package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class Main extends Activity {
	private static final String LogTag = "myLogs";
	//private Utils utils = new Utils();
	String s;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		Toast.makeText(this, getResources().getString(R.string.cant_run), Toast.LENGTH_SHORT).show();
		// finish();

		//new Thread(new RunnableCmd("")).start();

		/*
		 * RunnableCmd c = new RunnableCmd(""); try { Log.d(LogTag,
		 * c.CheckRoot() + ""); } catch (IOException e) { e.printStackTrace(); }
		 */
		//Log.d(LogTag, ""+(s=null));

		finish();

	}

	public class RunnableCmd implements Runnable {
		private Process process;
		private String SU;
		private Boolean need_wait = true;

		public RunnableCmd(String cmd) {

		}

		public void WaitForAnswer() {
			BufferedReader stdout;
			BufferedWriter stdin;
			Process ps = null;
			String pr;
			String line;
			try {
				ps = new ProcessBuilder("su").redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("logcat -c -b radio").append('\n');
				stdin.append("logcat -b radio RILJ:D *:S").append('\n');
				stdin.flush();
				stdin.close();

				stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
				while (((line = stdout.readLine()) != null) && (need_wait)) {
					if (line.matches(".*GET_CURRENT_CALLS.*(ACTIVE).*")) {
						break;
					}
				}
				pr = ps.toString();
				killproc(pr.substring(pr.indexOf('=') + 1, pr.indexOf(']')));
				ps.destroy();
				stdout.close();
				//Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				//v.vibrate(300);
				Log.d(LogTag,"VIBRATE");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public String getChilds(String ppid) throws IOException {
			// рекурсивный поиск всех дочерних процессов
			BufferedReader stdout;
			BufferedWriter stdin;
			Process ps;
			String[] psinfo;
			StringBuilder sb = new StringBuilder();

			ps = new ProcessBuilder(SU).redirectErrorStream(true).start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			try {
				stdin.append("toolbox ps").append('\n');
				stdin.flush();
			} finally {
				try {
					stdin.close();
				} catch (Exception e) {
				}
			}
			stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			try {
				String line = stdout.readLine();
				while (line != null) {
					psinfo = line.split(" +");
					if (psinfo[2].equals(ppid)) {
						sb.append(psinfo[1]).append(' ');
						sb.append(getChilds(psinfo[1]));
					}
					line = stdout.readLine();
				}
			} finally {
				try {
					stdout.close();
				} catch (Exception e) {
				}
			}
			return sb.toString();
		}

		public void killproc(String pid) throws IOException {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process kp;
			String cpid;

			kp = new ProcessBuilder(SU).redirectErrorStream(true).start();
			stdin = new BufferedWriter(new OutputStreamWriter(kp.getOutputStream()));
			try {
				cpid = getChilds(pid);
				stdin.append("kill -9 " + pid + " " + cpid).append('\n');
				stdin.flush();
			} finally {
				try {
					stdin.close();
				} catch (Exception e) {
				}
			}
		}

		public void run() {
			//Log.d(LogTag, utils.CheckRoot() + "");
			WaitForAnswer();

			/*
			 * Message msg; String line; BufferedReader stdout; BufferedWriter
			 * stdin; char[] buffer = new char[1024 * 4]; StringBuilder sb = new
			 * StringBuilder();
			 * 
			 * try { // SU = need_root ? "su" : "sh";
			 * 
			 * process = new
			 * ProcessBuilder(SU).redirectErrorStream(true).start(); stdin = new
			 * BufferedWriter(new
			 * OutputStreamWriter(process.getOutputStream())); try { if
			 * (cmd.contains(".sh")) { String fn = cmd.substring(0,
			 * cmd.lastIndexOf(".sh") + 3).replace(" ", "\\ "); String param =
			 * cmd.substring(cmd.lastIndexOf(".sh") + 3).trim();
			 * sb.append("sh").append(' '); cmd = fn + ' ' + param; }
			 * stdin.append(sb.append(cmd).append('\n').toString());
			 * stdin.flush(); } finally { try { stdin.close(); } catch
			 * (Exception e) { } }
			 * 
			 * running = true;
			 * 
			 * stdout = new BufferedReader(new
			 * InputStreamReader(process.getInputStream())); try { while
			 * (running && (-1 != stdout.read(buffer))) { line = new
			 * String(buffer).replace("\0", ""); // msg =
			 * save.handler.obtainMessage(MSG_OUT, 0, 0, // line); //
			 * save.handler.sendMessage(msg); Arrays.fill(buffer, '\0'); } }
			 * finally { try { stdout.close(); } catch (Exception e) { }
			 * 
			 * } running = false;
			 * 
			 * // запустить процесс автозакрытия if (process.waitFor() == 0) {
			 * // msg = save.handler.obtainMessage(MSG_TIME_CLOSE, //
			 * TIME_CLOSE_SECS, 0, null); // save.handler.sendMessage(msg); } }
			 * catch (Exception e) { } finally { this.stop(); }
			 */
		}

		public void stop() {
			// убить процессы в отдельном потоке, чтобы разгрузить основной
			// поток

			// running = false;

			new Thread(new Runnable() {
				public void run() {
					if (process != null) {
						String pr = process.toString();
						try {
							killproc(pr.substring(pr.indexOf('=') + 1, pr.indexOf(']')));
						} catch (IOException e) {
						}
					}
				}
			}).start();
		}
	}

}
