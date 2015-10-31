package org.oc.orchestra.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.oc.orchestra.client.Client;
import org.restlet.Component;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Daemon {
	private Component orchestraServer;
	Map<String, String> properties = new HashMap<String, String>();
	private static final transient Logger logger = LoggerFactory.getLogger(Server.class);
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
        InputStream is = this.getClass().getResourceAsStream("/server.conf");
        try {
			conf.load(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(String key : conf.stringPropertyNames()) {
			properties.put(key, conf.getProperty(key));
		}
		
	}

	public void start() throws Exception {
		System.out.println("Starting orchestra server.");
		org.restlet.Server server = orchestraServer.getServers().add(Protocol.HTTPS, Integer.valueOf(properties.get("port")));
		for(Entry<String, String> entry : properties.entrySet()) {
			logger.info(entry.getKey() + "=" + entry.getValue());
		}
			
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