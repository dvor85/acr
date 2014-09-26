package com.telephony.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

class DesEncrypter {
	Cipher ecipher;
	Cipher dcipher;

	/**
	 * Конструктор
	 * 
	 * @param key
	 *            секретный ключ алгоритма DES. Экземпляр класса SecretKey
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws UnsupportedEncodingException 
	 */
	public DesEncrypter(String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException {		
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF8"), "AES/ECB/NoPadding");
		ecipher = Cipher.getInstance("AES/ECB/NoPadding");
		dcipher = Cipher.getInstance("AES/ECB/NoPadding");
		ecipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		dcipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
	}

	/**
	 * Функция шифровнаия
	 * 
	 * @param str
	 *            строка открытого текста
	 * @return зашифрованная строка в формате Base64
	 */
	public String encrypt(String str) throws UnsupportedEncodingException, IllegalBlockSizeException,
			BadPaddingException {
		byte[] utf8 = str.getBytes("UTF8");
		byte[] enc = ecipher.doFinal(utf8);
		return Base64.encodeToString(enc, Base64.DEFAULT);
	}

	/**
	 * Функция расшифрования
	 * 
	 * @param str
	 *            зашифрованная строка в формате Base64
	 * @return расшифрованная строка
	 */
	public String decrypt(String str) throws IOException, IllegalBlockSizeException, BadPaddingException {
		byte[] dec = Base64.decode(str, Base64.DEFAULT);
		byte[] utf8 = dcipher.doFinal(dec);
		return new String(utf8, "UTF8");
	}
}