package org.magic.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class VersionChecker {

	DocumentBuilderFactory builderFactory;
	DocumentBuilder builder;
	Document document;
	NodeList nodeList;
	
	String urlVersion ="https://raw.githubusercontent.com/nicho92/MtgDesktopCompanion/master/src/res/version";
	String actualVersion = MTGDesktopCompanionControler.getInstance().getVersion();
	String onlineVersion;
	
	static final Logger logger = LogManager.getLogger(VersionChecker.class.getName());

	
	public VersionChecker() {
		builderFactory =DocumentBuilderFactory.newInstance();
		try {
			
			InputStream input = new URL(urlVersion).openConnection().getInputStream();
			BufferedReader read = new BufferedReader(new InputStreamReader(input));
			try {
				onlineVersion= read.readLine();
			} catch (IOException e) {
				onlineVersion="";
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public boolean hasNewVersion()
	{
		try{
			logger.info("check new version of app ");
			return Double.parseDouble(onlineVersion) > Double.parseDouble(actualVersion);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
			return false;
		}
	}

	public String getOnlineVersion() {
		return onlineVersion;
	}

	
	
	
}
