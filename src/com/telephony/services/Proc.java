package com.telephony.services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/**
 * Class for managing process
 * 
 * @author Dmitriy
 */
public class Proc {
	private String shell;
	protected String ppid;

	/**
	 * @param shell
	 *            sh or su
	 */
	public Proc(String shell) {
		this.shell = shell;
	}

	/**
	 * ���������� ���������� �������� � ��������� ������
	 * 
	 * @param ps
	 */
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

	/**
	 * ��������� ������� �� ������� cmds
	 * 
	 * @param cmds
	 *            ������ ������
	 * @return ���������� ����� stdout � stderr � ������ �����
	 * @throws IOException
	 */
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
			if (stdin != null) {
				stdin.close();
			}
			if (stdout != null) {
				stdout.close();
			}
			processDestroy(ps);
		}
		return res.toString().split("\r*\n+");
	}

	/**
	 * ����������� ����� ���� �������� ���������
	 * 
	 * @param ppid
	 *            parent pid
	 * @return pids with space as separator
	 * @throws IOException
	 */
	public String getChilds(String ppid) throws IOException {
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
	 * ������� ������ ���������
	 * 
	 * @param ppid
	 *            parent pid
	 * @throws IOException
	 */
	public void killTree(String ppid) throws IOException {
		String cpid = getChilds(ppid);
		exec(new String[] { "kill -9 " + ppid + " " + cpid });
	}

	/**
	 * ����� ���� �������
	 * 
	 * @param pid
	 * @throws IOException
	 */
	public void kill(String pid) throws IOException {
		// ����� �������
		// ������ ��� � ������� ����������� ��������. Process.destroy
		// ������� ������ � ����������������� ������� ???
		exec(new String[] { "kill -9 " + pid });
	}

}
