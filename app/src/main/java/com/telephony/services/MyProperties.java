package com.telephony.services;

import java.util.Properties;

public class MyProperties extends Properties {

    /**
     *
     */
    private static final long serialVersionUID = 4158587047003541874L;

    public Integer getIntProperty(String name) {
        return Integer.valueOf(super.getProperty(name));
    }

    public Integer getIntProperty(String name, Integer defaultValue) {
        return Integer.valueOf(super.getProperty(name, defaultValue.toString()));
    }

    public Boolean getBoolProperty(String name) {
        return Boolean.valueOf(super.getProperty(name));
    }

    public Boolean getBoolProperty(String name, Boolean defaultValue) {
        return Boolean.valueOf(super.getProperty(name, defaultValue.toString()));
    }

    public String[] getStringsProperty(String name) {
        return (super.containsKey(name)) ? super.getProperty(name).split(" *; *") : null;
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
