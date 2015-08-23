package org.oc.orchestra.client;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.auth.KeystoreHelper;

public class ClientTarget extends Target {
	private static final String PRIVATE_KEY = "privateKey";
	private static final String CERT = "cert";

	public ClientTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		builder.setNeedAuthHeader(true).setTarget("client");
		String name = null;
		String keystorename = null;
		String keystore_password = null;
		String[] args = cmd.getArgs();
		if(args.length > 2) name = args[2];
		if(!method.equals("list")) {
			if(method.equals("install") || method.equals("uninstall")) {
				//client name should be hostname when install or uninstall
				name = Client.getName();
				keystorename = cmd.hasOption("keystore") ? 
						cmd.getOptionValue("keystore") : "keystore/clientKey.jks";
				keystore_password = cmd.hasOption("keystore_password") ? 
						cmd.getOptionValue("keystore_password") : "password";
			}
			if(name == null) ArgsHelper.usage();
		}
		
		if(!method.equals("list")) builder.addPathParameter(name);
		if(method.equals("list")) {
			builder.setMethod("get").setTarget("clients");
		} else if(method.equals("delete")) {
			builder.setMethod("delete");
		} else if(method.equals("get")) {
			builder.setMethod("get");
		} else if(method.equals("create")) {
			builder.setMethod("post");
		} else if(method.equals("update")) {
			builder.setMethod("put");
		} else if(method.equals("install")) {
			builder.setMethod("post");
		} else if(method.equals("uninstall")) {
			builder.setMethod("delete");
		}
		
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		if(method.equals("install")) {
			if(response.getStatusLine().getStatusCode() == 409) {
				throw new RuntimeException("Client already exists.");
			} else if(response.getStatusLine().getStatusCode() == 201) {
				JSONObject json = null;
				try {
					json = (JSONObject) JSONValue.parse(
							new InputStreamReader(response.getEntity().getContent()));
				} catch (IllegalStateException | IOException e1) {
					e1.printStackTrace();
				}
				System.out.println(json.toString());

				String encodedPk = (String) json.get(PRIVATE_KEY);
				// initializing and clearing keystore 
				String alias = name;
				String encodeCert = (String) json.get(CERT);
				
				KeystoreHelper helper;
				try {
					helper = new KeystoreHelper(keystorename, keystore_password);
					helper.savePrivateKey(encodedPk, alias, keystore_password, encodeCert);
				} catch (KeyStoreException | NoSuchAlgorithmException
						| CertificateException | IOException | InvalidKeySpecException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		if(method.equals("delete")) {
			if(response.getStatusLine().getStatusCode() == 204) {
		    	KeystoreHelper helper;
				try {
					helper = new KeystoreHelper(keystorename, keystore_password);
					helper.deleteCertificate(name);
				} catch (KeyStoreException | NoSuchAlgorithmException
						| CertificateException | IOException e) {
					e.printStackTrace();
				}
		    } else if(response.getStatusLine().getStatusCode() == 404) {
		    	throw new RuntimeException("Client does not exist.");
		    }
		}
	    output(response);
	}
}
