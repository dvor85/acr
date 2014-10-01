package com.telephony.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyProperties extends Properties {

	public synchronized void load(String instr) throws IOException {
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(instr.getBytes("UTF8"));
			super.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public Integer getIntProperty(String name) {
		if (super.containsKey(name)) {
			return Integer.parseInt(super.getProperty(name));
		} else {
			return null;
		}
	}

	public Integer getIntProperty(String name, Integer defaultValue) {
		return Integer.parseInt(super.getProperty(name, defaultValue.toString()));
	}

	public Boolean getBoolProperty(String name) {
		if (super.containsKey(name)) {
			return Boolean.parseBoolean(super.getProperty(name));
		} else {
			return null;
		}
	}

	public Boolean getBoolProperty(String name, Integer defaultValue) {
		return Boolean.parseBoolean(super.getProperty(name, defaultValue.toString()));
	}

	public String[] getStringsProperty(String name) {
		if (super.containsKey(name)) {
			return super.getProperty(name).split(" *; *");
		} else {
			return null;
		}
	}

	public void setIntProperty(String name, Integer value) {
		super.setProperty(name, value.toString());
	}

	public void setBoolProperty(String name, Boolean value) {
		super.setProperty(name, value.toString());
	}

	public void setStringsProperty(String name, String[] value) {
		super.setProperty(name, Utils.implodeStrings(value, ";"));
	}

}
