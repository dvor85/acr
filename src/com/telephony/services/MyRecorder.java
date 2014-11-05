package com.telephony.services;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;

public class MyRecorder extends MediaRecorder {
	private boolean started = false;

	private static int[] AUDIO_SOURCES = { MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.VOICE_UPLINK,
			MediaRecorder.AudioSource.VOICE_DOWNLINK, MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.DEFAULT,
			MediaRecorder.AudioSource.MIC };

	/**
	 * �������� �� ������.
	 * 
	 * @return true - ���� ������ ����.
	 */
	public synchronized boolean isStarted() {
		return started;
	}

	/**
	 * �������� ������. �������������� ���������� ��������� ������ �� ������� AUDIO_SOURCES
	 * 
	 * @param source
	 *            �������������� �������� ������. ���� �� �� �������������� (���������� ����������), ���������� ��������� �� ������� AUDIO_SOURCES.
	 * @param file
	 *            ���� ��� ������
	 * @param max_duration
	 *            ������������ ������������ ������ � �������������
	 * @throws IOException
	 *             ���� �� ���� �������� ������ �� ��������.
	 */
	public synchronized void startRecorder(int source, File file, int max_duration) throws IOException {
		if (started) {
			return;
		}
		int i = 0, k = AUDIO_SOURCES.length;

		for (int s : AUDIO_SOURCES) {
			if (s == source) {
				k = i;
			}
			if (i >= k) {
				try {
					reset();
					setAudioSource(s);
					setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
					setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					setMaxDuration(max_duration);
					setOutputFile(file.getAbsolutePath());

					prepare();
					start();
					Log.d(Utils.LOG_TAG, "source = " + s + " index = " + i);
					return;
				} catch (Exception e) {
				}
			}
			i++;
		}
		throw new IOException("Error while start media recorder!");
	}

	@Override
	public void start() throws IllegalStateException {
		super.start();
		started = true;
	}

	@Override
	public void stop() throws IllegalStateException {
		if (started) {
			super.stop();
			started = false;
		}
	}

	@Override
	public void reset() {
		if (started) {
			stop();
		}
		super.reset();
	}

	@Override
	public void release() {
		if (started) {
			reset();
		}
		super.release();
	}

	public void eraseFileIfLessThan(File file, long size) {
		if ((file != null) && (file.length() < size)) {
			file.delete();
		}
	}

}
