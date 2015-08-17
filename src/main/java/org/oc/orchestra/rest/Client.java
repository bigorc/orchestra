package org.oc.orchestra.rest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.apache.shiro.codec.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.auth.CertificateHelper;
import org.oc.orchestra.auth.Constants;
import org.oc.orchestra.auth.KeystoreHelper;
import org.oc.orchestra.auth.ServerAuthHelper;
import org.oc.orchestra.dao.ClientDao;
import org.oc.util.LocalCommand;
import org.oc.util.LocalCommandBuilder;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

@Component
public class Client extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Client.class);
	private static final String SERVER_TRUSTSTORE = "keystore/serverTrust.jks";
	private static final String algorithm = "SHA1withRSA";
	private static final int days = 365;
	
	@Post
	public Representation createClient() throws JSONException, GeneralSecurityException, IOException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.info("client authenticated:" + getRequest().getClientInfo().isAuthenticated());
		String updated_by = AuthFilter.getUserPass(getRequest())[0];
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.oc.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			client = new org.oc.orchestra.dao.Client();
			client.setCreator(updated_by);
			client.setName(clientname);
			clientDao.create(client);
			String dn = "CN=" + clientname;
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		    keyGen.initialize(2048);
			KeyPair pair = keyGen.generateKeyPair();
			
			X509Certificate cert = CertificateHelper.generateCertificate(dn, pair, days, algorithm);
			String password = "password";
			KeystoreHelper helper = new KeystoreHelper(SERVER_TRUSTSTORE , password );
		    helper.saveCertificate(clientname, cert);
		    
			JSONObject json = new org.json.JSONObject();
			json.put("privateKey", Base64.encodeToString(pair.getPrivate().getEncoded()));
			json.put("cert", Base64.encodeToString(cert.getEncoded()));
			
			getResponse().setStatus(Status.SUCCESS_CREATED, "Client is updated.");
			return new JsonRepresentation(json );
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Client already exists.");
			return new StringRepresentation("Client already exists.");
		}
	}
	
	@Put
	public Representation updateClient() throws JSONException, GeneralSecurityException, IOException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		String apikey = getQuery().getValues("apikey");
		String updated_by = AuthFilter.getUserPass(getRequest())[0];
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.oc.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			client = new org.oc.orchestra.dao.Client();
			client.setCreator(updated_by);
			client.setName(clientname);
			clientDao.create(client);
		} else {
			client.setUpdated_by(updated_by);
			clientDao.update(client);
		}
		String dn = "CN=" + clientname;
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    keyGen.initialize(2048);
		KeyPair pair = keyGen.generateKeyPair();
		
		X509Certificate cert = CertificateHelper.generateCertificate(dn, pair, days, algorithm);
		String password = "password";
		KeystoreHelper helper = new KeystoreHelper(SERVER_TRUSTSTORE , password );
	    helper.saveCertificate(clientname, cert);
	    
		JSONObject json = new org.json.JSONObject();
		json.put("privateKey", Base64.encodeToString(pair.getPrivate().getEncoded()));
		json.put("cert", Base64.encodeToString(cert.getEncoded()));
		getResponse().setStatus(Status.SUCCESS_OK, "Client is updated.");
		return new JsonRepresentation(json );
	}

	@Get
	public Representation getClient() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, JSONException, UnrecoverableKeyException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.oc.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Client not found.");
			return null;
		}
		KeystoreHelper helper = new KeystoreHelper(SERVER_TRUSTSTORE , "password" );
		JSONObject json = new org.json.JSONObject();
		json.put("creator", client.getCreator());
		json.put("created_at", client.getCreated_at());
		json.put("cert", Base64.encodeToString(helper.loadCertificate(clientname).getEncoded()));
		getResponse().setStatus(Status.SUCCESS_OK, "Client info.");
		return new JsonRepresentation(json);
	}
	
	@Delete
	public Representation deleteClient() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, JSONException, UnrecoverableKeyException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.oc.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Client not found.");
			return null;
		}
		clientDao.delete(clientname);
		KeystoreHelper helper = new KeystoreHelper(SERVER_TRUSTSTORE , "password" );
		helper.deleteCertificate(clientname);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "Client is deleted.");
		return null;
	}
}
