package org.oc.orchestra.rest;

import org.restlet.Component;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

public class Server {
	private static final String TRUSTSTORE_PATH = "keystore/serverTrust.jks";
	private static final String KEYSTORE_PATH = "keystore/serverKey.jks";
	private Component orchestraServer;

	public Server() {
		orchestraServer = new Component();
	}
	
	public static void main(String[] args) throws Exception {
		new Server().start();
//		orchestraServer.getServers().add(Protocol.HTTP, 8111);
//		org.restlet.Server server = new org.restlet.Server(Protocol.HTTP, 8111);
//		server.setNext(new OrchestraApplication());
//		server.start();
	}

	public void start() throws Exception {
		org.restlet.Server server = orchestraServer.getServers().add(Protocol.HTTPS, 8183);
		
		Series<Parameter> parameters = server.getContext().getParameters();
		parameters.add("keystorePath", KEYSTORE_PATH);
		parameters.add("keystorePassword", "password");
		parameters.add("keystoreType", "JKS");
		parameters.add("keyPassword", "password");
		parameters.add("wantClientAuthentication", "true");
		parameters.add("truststorePath", TRUSTSTORE_PATH);
		parameters.add("truststorePassword", "password");
		parameters.add("truststoreType", "JKS");
		orchestraServer.getDefaultHost().attach(new OrchestraApplication());
		orchestraServer.start();
	}

	public void stop() throws Exception {
		orchestraServer.stop();
	}
}