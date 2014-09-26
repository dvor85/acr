package com.telephony.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import android.util.Base64;

class DesEncrypter {
	Cipher ecipher;
	Cipher dcipher;

	/**
	 * �����������
	 * 
	 * @param key
	 *            ��������� ���� ��������� DES. ��������� ������ SecretKey
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 */
	public DesEncrypter(SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		//key = KeyGenerator.getInstance("DES")
		ecipher = Cipher.getInstance("DES");
		dcipher = Cipher.getInstance("DES");
		ecipher.init(Cipher.ENCRYPT_MODE, key);
		dcipher.init(Cipher.DECRYPT_MODE, key);
	}

	/**
	 * ������� ����������
	 * 
	 * @param str
	 *            ������ ��������� ������
	 * @return ������������� ������ � ������� Base64
	 */
	public String encrypt(String str) throws UnsupportedEncodingException, IllegalBlockSizeException,
			BadPaddingException {
		byte[] utf8 = str.getBytes("UTF8");
		byte[] enc = ecipher.doFinal(utf8);
		return Base64.encodeToString(enc, Base64.DEFAULT);
	}

	/**
	 * ������� �������������
	 * 
	 * @param str
	 *            ������������� ������ � ������� Base64
	 * @return �������������� ������
	 */
	public String decrypt(String str) throws IOException, IllegalBlockSizeException, BadPaddingException {
		byte[] dec = Base64.decode(str, Base64.DEFAULT);
		byte[] utf8 = dcipher.doFinal(dec);
		return new String(utf8, "UTF8");
	}
}