package com.rinke.solutions.godmd.flash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Slf4j
public class ApplicationProperties {
	
	private String propertiesFilename = "godmdflash.properties";
	private static ApplicationProperties theInstance;

	public static synchronized ApplicationProperties getInstance() {
		if (theInstance == null) {
			theInstance = new ApplicationProperties();
			theInstance.load();
		}
		return theInstance;
	}

	private ApplicationProperties() {
		super();
	}

	private Properties props = new Properties();

	private String getFilename() {
		String homeDir = System.getProperty("user.home");
		String filename = homeDir + File.separator + propertiesFilename;
		return filename;
	}

	public void load() {
		String filename = getFilename();
		try {
			props.load(new FileInputStream(filename));
			log.info("loaded properties from "+ filename);
		} catch( FileNotFoundException e ) {
			log.info("no property file "+ filename + " found" );
		} catch (Exception e) {
			log.warn("problems loading "+filename+" from ", e);
		}
	}

	public static Properties getProperties() {
		return getInstance().props;
	}

	public static void put(String key, String value) {
		log.info("setting prop "+key+" to '"+value+"'");
		String old = getInstance().props.getProperty(key);
		if( !value.equals(old) ) {
			log.info("value for prop "+key+" changed "+old+" -> "+value+" ");
			getInstance().props.put(key, value);
			getInstance().save();
		}
	}

	public void save() {
		String filename = getFilename();
		try {
			props.store(new FileOutputStream(filename), "");
		} catch (IOException e) {
			log.error("storing "+ filename);
			throw new RuntimeException(e);
		}
	}

	public static String get(String key) {
		String val = getInstance().props.getProperty(key);
		log.info("get prop "+key+" = '"+val+"' ");
		return val;
	}

	/** mainly for testing purpose */
	public static void setPropFile(String filename) {
		getInstance().propertiesFilename = filename;
		
	}

	public static boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public static boolean getBoolean(String key, boolean defaultVal) {
		String val = get(key);
		return val!=null?Boolean.parseBoolean(val):defaultVal;	
	}
}
