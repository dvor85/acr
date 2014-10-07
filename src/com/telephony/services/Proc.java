package com.telephony.services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/**
 * Class for kill pid using shell
 * 
 * @author Dmitriy
 *
 */
public class Proc {
	private String shell;
	protected String ppid;

	/**
	 * @param shell
	 *            - sh or su
	 */
	public Proc(String shell) {
		this.shell = shell;
	}

	public static void processDestroy(Process ps) {
		try {
			if (ps != null) {
				ps.exitValue();
			}
		} catch (IllegalThreadStateException e) {
			ps.destroy();
		} finally {
			ps = null;
		}
	}

	public String[] exec(String[] cmds) throws IOException {
		byte[] buffer = new byte[1024];
		Process ps = null;
		InputStream stdout = null;
		BufferedWriter stdin = null;
		StringBuilder res = new StringBuilder();
		try {
			ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
			ppid = ps.toString().substring(ps.toString().indexOf('=') + 1, ps.toString().indexOf(']'));
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			for (String cmd : cmds) {				
				stdin.append(cmd).append('\n');
			}
			stdin.flush();
			stdin.close();
			
			stdout = ps.getInputStream();
			int count = -1;
			while ((count = stdout.read(buffer)) > 0) {
				res.append(new String(buffer).substring(0, count));
			}			
		} finally {
			if (stdin!=null) {
				stdin.close();
			}
			if (stdout!=null) {
				stdout.close();
			}
			processDestroy(ps);
		}
		return res.toString().split("\r*\n+");
	}

	/**
	 * Recursive search pid with ppid as parent
	 * 
	 * @param ppid
	 *            - parent pid
	 * @return pids with space as separator
	 * @throws IOException
	 */
	public String getChilds(String ppid) throws IOException {
		// рекурсивный поиск всех дочерних процессов
		String[] psinfo, ps;
		StringBuilder sb = new StringBuilder();

		ps = exec(new String[] { "toolbox ps | toolbox grep " + ppid });
		for (String psline : ps) {
			psinfo = psline.split(" +");
			if (psinfo[2].equals(ppid)) {
				sb.append(psinfo[1]).append(' ');
				sb.append(getChilds(psinfo[1]));
			}
		}
		return sb.toString();
	}

	/**
	 * Kill tree of pids
	 * 
	 * @param ppid
	 *            - parent pid
	 * @throws IOException
	 */
	public void killTree(String ppid) throws IOException {
		// убить процесс и все дочерние процессы
		// делать это с правами запущенного процесса. Process.destroy
		// убивает только с пользовательскими правами ???
		String cpid = getChilds(ppid);
		exec(new String[] { "kill -9 " + ppid + " " + cpid });
	}

	/**
	 * Kill one proc by pid
	 * 
	 * @param pid
	 * @throws IOException
	 */
	public void kill(String pid) throws IOException {
		// убить процесс
		// делать это с правами запущенного процесса. Process.destroy
		// убивает только с пользовательскими правами ???
		exec(new String[] { "kill -9 " + pid });
	}

}
