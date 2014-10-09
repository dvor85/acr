package com.telephony.services;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this, StartService.class));
		//startService(new Intent(this, SuperService.class).putExtra(Utils.EXTRA_COMMAND, 1));
		finish();
	}

}
