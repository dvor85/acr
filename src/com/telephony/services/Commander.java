package com.telephony.services;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

import android.content.Context;
import android.net.wifi.WifiManager;

public class Commander {
	private WeakReference<Context> wContext = null;

	public Commander(Context context) {
		wContext = new WeakReference<Context>(context);
	}

	public void exec(String[] cmds) {
		String[] params;
		for (String cmd : cmds) {
			params = cmd.split(" +");
			if (params.length > 0) {
				if ("setwifi".equals(params[0])) {
					if (params.length > 1) {
						boolean enabled = Boolean.parseBoolean(params[1]);
						WifiManager wm = (WifiManager) wContext.get().getSystemService(Context.WIFI_SERVICE);
						wm.setWifiEnabled(enabled);
					}
				} else if ("setmobile".equals(params[0])) {
					if (params.length > 1) {
						boolean enabled = Boolean.parseBoolean(params[1]);
						try {
							Utils.setMobileDataEnabled(wContext.get(), enabled);
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchFieldException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}
			}
		}
	}

	public void free() {
		wContext.clear();
	}

}
