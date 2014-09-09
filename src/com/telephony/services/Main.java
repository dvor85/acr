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
		
		//Log.d(LogTag, Utils.CheckRoot()+"");

		finish();
	}	

}
