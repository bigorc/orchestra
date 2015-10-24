package org.oc.orchestra.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.restlet.Component;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

public class Server implements Daemon {
	private Component orchestraServer;
	Map<String, String> properties = new HashMap<String, String>();

	public Server() {
		orchestraServer = new Component();
	}
	
	public void config() {
		properties.put("truststore", "keystore/serverTrust.jks");
		properties.put("keystore", "keystore/serverKey.jks");
		properties.put("port", "8183");
		properties.put("keystore.password", "password");
		properties.put("key.password", "password");
		properties.put("truststore.password", "password");
		Properties conf = new Properties();
        InputStream is;
		try {
			if(new File("conf/server.conf").exists()) {
			 is = new FileInputStream("conf/server.conf");
			} else if(new File("/etc/orchestra/server.conf").exists()) {
				is = new FileInputStream("/etc/orchestra/server.conf");
			} else {
				return;
			}
			conf.load(is);
			for(String key : conf.stringPropertyNames()) {
				properties.put(key, conf.getProperty(key));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() throws Exception {
		System.out.println("Stopping orchestra server.");
		org.restlet.Server server = orchestraServer.getServers().add(Protocol.HTTPS, Integer.valueOf(properties.get("port")));
		
		Series<Parameter> parameters = server.getContext().getParameters();
		parameters.add("keystorePath", properties.get("keystore"));
		parameters.add("keystorePassword", properties.get("keystore.password"));
		parameters.add("keystoreType", "JKS");
		parameters.add("keyPassword", properties.get("key.password"));
		parameters.add("wantClientAuthentication", "true");
		parameters.add("truststorePath", properties.get("truststore"));
		parameters.add("truststorePassword", properties.get("truststore.password"));
		parameters.add("truststoreType", "JKS");
		orchestraServer.getDefaultHost().attach(new OrchestraApplication());
		orchestraServer.start();
	}

	public void stop() throws Exception {
		System.out.println("Stopping orchestra server.");
		orchestraServer.stop();
	}

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.config();
		server.start();
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		config();
	}

}