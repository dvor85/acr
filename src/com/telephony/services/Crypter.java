package com.telephony.services;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

class Crypter {
	
	private static final String ALGORITHM = "AES";
	private static final SecretKeySpec secretKeySpec = new SecretKeySpec("9c6a5e77ec658f08".getBytes(), ALGORITHM);

	/**
	 * ������� ����������
	 * 
	 * @param str
	 *            ������ ��������� ������
	 * @return ������������� ������ � ������� Base64
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public static String encrypt(String str) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException  {
		Cipher cipher;
		cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		byte[] utf8 = str.getBytes("UTF8");
		byte[] enc = cipher.doFinal(utf8);
		return Base64.encodeToString(enc, Base64.DEFAULT);
	}

	/**
	 * ������� �������������
	 * 
	 * @param str
	 *            ������������� ������ � ������� Base64
	 * @return �������������� ������
	 * @throws UnsupportedEncodingException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
		
	public static String decrypt(String str) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException   {
		Cipher cipher;
		cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
		byte[] dec = Base64.decode(str, Base64.DEFAULT);
		byte[] utf8 = cipher.doFinal(dec);
		return new String(utf8, "UTF8");
	}
}