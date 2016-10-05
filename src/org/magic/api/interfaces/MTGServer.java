package org.magic.api.interfaces;

import java.io.IOException;
import java.util.Properties;

public interface MTGServer {

	
	public void start() throws Exception ;
	
	public void stop() throws Exception;
	
	public boolean isAlive() throws Exception;
	
	
	public Properties getProperties();
	public void setProperties(String k,Object value);
	public Object getProperty(String k);
	public String getName();
	public boolean isEnable();
	public void save();
	public void load();
	public void enable(boolean t);
	
	
}
