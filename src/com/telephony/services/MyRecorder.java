package com.telephony.services;

import android.media.MediaRecorder;

public class MyRecorder extends MediaRecorder {
	private Boolean started = false;
	
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
			stop();
		}
		super.release();		
	}
	

}
