package org.oc.orchestra.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpCommand {
	
	private static String KEYSTORE_PATH = "keystore/clientKey.jks";
	private static final String TRUSTSTORE_PATH = "keystore/clientTrust.jks";
	private HttpRequestBase request;
	private HttpClient httpclient;
	private boolean authenticate_client = true;
	
	public HttpCommand(HttpRequestBase request) {
		this.request = request;
		this.httpclient = getHttpClient();
	}

	public HttpCommand(HttpRequestBase request, boolean authenticate_client) {
		this.authenticate_client = authenticate_client;
		this.request = request;
		this.httpclient = getHttpClient();
	}

	public static void setKeystorePath(String keystore_path) {
		KEYSTORE_PATH = keystore_path;
	}
	
	private HttpClient getHttpClient() {
		HttpClient client = new DefaultHttpClient();
		Scheme sch = getScheme();
        client.getConnectionManager().getSchemeRegistry().register(sch);
        return client;
	}
	
	public HttpResponse execute() {
		try {
			return httpclient.execute(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	private Scheme getScheme() {
		KeyStore keyStore;
		KeyStore trustStore;
		FileInputStream instream = null;
		FileInputStream keyStream = null;
		SSLSocketFactory socketFactory = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());        
	        instream = new FileInputStream(new File(TRUSTSTORE_PATH)); 
            trustStore.load(instream, "password".toCharArray());
            
            if(authenticate_client) {
	            keyStream = new FileInputStream(new File(KEYSTORE_PATH));
	            keyStore.load(keyStream, "password".toCharArray());
	            socketFactory = new SSLSocketFactory(keyStore, "password", trustStore);
            } else {
            	socketFactory = new SSLSocketFactory(trustStore);
            }
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException | UnrecoverableKeyException e) {
			e.printStackTrace();
		} finally {
            try {
				instream.close();
				if(keyStream != null) keyStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
        };
        
        Scheme sch = new Scheme("https", socketFactory, 8183);
		return sch;
	}

}
