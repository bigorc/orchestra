package org.orchestra.client;
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
import org.orchestra.auth.KeystoreHelper;

public class ClientTarget extends Target {
	public static final String PRIVATE_KEY = "privateKey";
	public static final String CERT = "cert";

	public ClientTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	public KeystoreHelper getKeystoreHelper() {
		try {
			return new KeystoreHelper(System.getProperty("javax.net.ssl.keyStore"), 
					System.getProperty("javax.net.ssl.keyStorePassword"));
		} catch (KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void execute(String method, CommandLine cmd) {
		builder.setNeedAuthHeader(true).setTarget("client");
		String name = null;
		String[] args = cmd.getArgs();
		if(args.length > 2) name = args[2];
		KeystoreHelper helper = getKeystoreHelper();
		if(name == null) name = Client.getName();
		
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
			new Client().update();
			return;
		} else if(method.equals("install")) {
			builder.setMethod("post");
		} else if(method.equals("uninstall")) {
			builder.setMethod("delete");
		}
		
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		if(method.equals("install")) {
			if(response.getStatusLine().getStatusCode() == 409) {
				System.out.println("Client already exists.");
				try {
					if(!helper.containsCertificate(name)) {
						System.out.println("Client is already created on server, but the certificate is missing");
						System.exit(1);
					}
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}
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
				
				try {
					helper.savePrivateKey(encodedPk, alias, null, encodeCert);
				} catch (KeyStoreException | NoSuchAlgorithmException
						| CertificateException | IOException | InvalidKeySpecException e) {
					e.printStackTrace();
				}
			}
			new Client().createParents();
			output(response);
			return;
		}
		if(method.equals("uninstall")) {
			if(response.getStatusLine().getStatusCode() == 204) {
				new ApikeyTarget().execute("delete", null);
			}
			new ClientAuthHelper(Client.getUsername(), Client.getPassword()).removeApikeyFile();
			try {
				helper.deleteCertificate(name);
			} catch (KeyStoreException | NoSuchAlgorithmException
					| CertificateException | IOException e) {
				e.printStackTrace();
			}
			new Client().deleteParents();
		}
	    output(response);
	}
}
