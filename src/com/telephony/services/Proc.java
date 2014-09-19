package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

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

	public String[] exec(String[] cmds) throws IOException {
		Process ps = null;
		BufferedReader stdout;
		BufferedWriter stdin;
		String line;
		ArrayList<String> res = new ArrayList<String>();
		try {
			ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
			ppid = ps.toString().substring(ps.toString().indexOf('=') + 1, ps.toString().indexOf(']'));
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			for (String cmd : cmds) {
				stdin.append(cmd).append('\n');
			}
			stdin.flush();
			stdin.close();
			stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			while (((line = stdout.readLine()) != null)) {
				res.add(line);
			}
		} finally {
			if (ps != null) {
				kill(ppid);
				ps.destroy();
			}
		}
		return res.toArray(new String[res.size()]);
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

		ps = exec(new String[] { "toolbox ps" });
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
		// убивает только с пользовательскими правами
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
		// убивает только с пользовательскими правами
		exec(new String[] { "kill -9 " + pid });
	}

}
