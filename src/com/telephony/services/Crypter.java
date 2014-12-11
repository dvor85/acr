package com.telephony.services;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

class Crypter {

	private static final String ALGORITHM = "AES";

	private static SecretKeySpec getKey(String skey) throws UnsupportedEncodingException {
		byte[] buffer;
		byte[] key = new byte[0];
		if (skey != null) {
			key = skey.getBytes("UTF8");
		}
		buffer = Arrays.copyOf(key, 16);
		return new SecretKeySpec(buffer, ALGORITHM);
	}

	/**
	 * Функция шифрования
	 * 
	 * @param str
	 *            Строка открытого текста
	 * @param key
	 *            Ключевая фраза
	 * @return зашифрованная строка в формате Base64
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public static String encrypt(String str, String key) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher cipher;
		cipher = Cipher.getInstance(ALGORITHM);

		cipher.init(Cipher.ENCRYPT_MODE, getKey(key));
		byte[] utf8 = str.getBytes("UTF8");
		byte[] enc = cipher.doFinal(utf8);
		return Base64.encodeToString(enc, Base64.DEFAULT);
	}

	/**
	 * Функция расшифрования
	 * 
	 * @param str
	 *            Зашифрованная строка в формате Base64
	 * @param key
	 *            Ключевая фраза
	 * @return расшифрованная строка
	 * @throws UnsupportedEncodingException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 */

	public static String decrypt(String str, String key) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher cipher;
		cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, getKey(key));
		byte[] dec = Base64.decode(str, Base64.DEFAULT);
		byte[] utf8 = cipher.doFinal(dec);
		return new String(utf8, "UTF8");
	}
}