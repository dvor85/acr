package com.telephony.services;

import java.io.File;

import android.media.MediaRecorder;

public class MyRecorder extends MediaRecorder {
	protected Boolean started = false;

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
