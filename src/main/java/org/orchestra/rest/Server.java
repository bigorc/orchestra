package org.orchestra.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.orchestra.auth.ReloadableSslContextFactory;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Daemon {
	private static Component orchestraServer;
	static Map<String, String> properties = new HashMap<String, String>();
	private static final transient Logger logger = LoggerFactory.getLogger(Server.class);
	
	static {
		properties.put("truststore", "keystore/serverTrust.jks");
		properties.put("keystore", "keystore/serverKey.jks");
		properties.put("port", "8183");
		properties.put("keystore.password", "password");
		properties.put("key.password", "password");
		properties.put("key.size", "2048");
		properties.put("truststore.password", "password");
		properties.put("ro.dir", "ro");
		properties.put("nonce.expiration.period", "15");
		properties.put("nonce.deletion.period", "1");
		properties.put("synctime.limit", "5");
		properties.put("mongodb.host", "localhost");
		properties.put("mongodb.port", "27017");
		properties.put("mongodb.user", "orchestra");
		properties.put("mongodb.password", "orchestra");
	}
	
	public Server() {
		if(orchestraServer == null) orchestraServer = new Component();
	}
	
	public void config() {
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

	public static String getProperty(String key) {
		return properties.get(key);
	}
	
	public void start() throws Exception {
		System.out.println("Starting orchestra server.");
		org.restlet.Server server = orchestraServer.getServers().add(
				Protocol.HTTPS, Integer.valueOf(properties.get("port")));
		for(Entry<String, String> entry : properties.entrySet()) {
			logger.debug(entry.getKey() + "=" + entry.getValue());
		}
		
		Context context = server.getContext();
		Series<Parameter> parameters = context.getParameters();
		parameters.add("keystorePath", properties.get("keystore"));
		parameters.add("keystorePassword", properties.get("keystore.password"));
		parameters.add("keystoreType", "JKS");
		parameters.add("keyPassword", properties.get("key.password"));
		parameters.add("wantClientAuthentication", "true");
		parameters.add("truststorePath", properties.get("truststore"));
		parameters.add("truststorePassword", properties.get("truststore.password"));
		parameters.add("truststoreType", "JKS");
//		parameters.add("sslContextFactory", "org.orchestra.auth.ReloadableSslContextFactory");
		Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
		ReloadableSslContextFactory sslContextFacotry = new ReloadableSslContextFactory();
		sslContextFacotry.init(parameters);
		attributes.put("sslContextFactory", sslContextFacotry);
		context.setAttributes(attributes);
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
	
	public static Component getServer() {
		return orchestraServer;
	}

	public static ReloadableSslContextFactory getSslContextFactory() {
		return (ReloadableSslContextFactory) orchestraServer.getServers().get(0).getContext().getAttributes().get("sslContextFactory");
	}
}