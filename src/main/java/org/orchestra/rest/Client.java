package org.orchestra.rest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.shiro.codec.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.orchestra.auth.CertificateHelper;
import org.orchestra.auth.KeystoreHelper;
import org.orchestra.dao.ClientDao;
import org.orchestra.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

@Component
public class Client extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Client.class);
	private static final String algorithm = "SHA1withRSA";
	private static final int days = 365;
	
	@Post
	public Representation createClient() throws JSONException, GeneralSecurityException, IOException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("client:" + clientname);
		String updated_by = AuthFilter.getUserPass(getRequest())[0];
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			//create client database record
			client = new org.orchestra.dao.Client();
			client.setCreator(updated_by);
			client.setName(clientname);
			clientDao.create(client);
			
			KeystoreHelper helper = new KeystoreHelper(Server.getProperty("truststore"),
					Server.getProperty("truststore.password"));
		    if(helper.containsCertificate(clientname)) {
		    	helper.deleteCertificate(clientname);
		    }
			String dn = "CN=" + clientname;
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		    keyGen.initialize(Integer.valueOf(Server.getProperty("key.size")));
			KeyPair pair = keyGen.generateKeyPair();
			
			X509Certificate cert = CertificateHelper.generateCertificate(dn, pair, days, algorithm);
			helper.saveCertificate(clientname, cert);
		    
			JSONObject json = new org.json.JSONObject();
			json.put("privateKey", Base64.encodeToString(pair.getPrivate().getEncoded()));
			json.put("cert", Base64.encodeToString(cert.getEncoded()));
			
			getResponse().setStatus(Status.SUCCESS_CREATED, "Client is created.");
			return new JsonRepresentation(json );
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Client already exists.");
			return new StringRepresentation("Client already exists.");
		}
	}
	
	@Put
	public Representation updateClient() throws JSONException, GeneralSecurityException, IOException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("client:" + clientname);
		String updated_by = AuthFilter.getUserPass(getRequest())[0];
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			client = new org.orchestra.dao.Client();
			client.setCreator(updated_by);
			client.setName(clientname);
			clientDao.create(client);
		} else {
			client.setUpdated_by(updated_by);
			clientDao.update(client);
		}
		String dn = "CN=" + clientname;
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    keyGen.initialize(Integer.valueOf(Server.getProperty("key.size")));
		KeyPair pair = keyGen.generateKeyPair();
		
		X509Certificate cert = CertificateHelper.generateCertificate(dn, pair, days, algorithm);
		KeystoreHelper helper = new KeystoreHelper(Server.getProperty("truststore") , Server.getProperty("truststore.password") );
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
		logger.debug("client:" + clientname);
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Client not found.");
			return null;
		}
		KeystoreHelper helper = new KeystoreHelper(Server.getProperty("truststore") , Server.getProperty("truststore.password") );
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
		logger.debug("client:" + clientname);
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		org.orchestra.dao.Client client = clientDao.getClient(clientname);
		if(client == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Client not found.");
			return null;
		}
		clientDao.delete(clientname);
		
		//remove apikeys of the client
		DBCollection col = ServerAuthHelper.getUserCollection();
		col.remove(new BasicDBObject("clientname", clientname));
		
		KeystoreHelper helper = new KeystoreHelper(Server.getProperty("truststore") , Server.getProperty("truststore.password"));
		helper.deleteCertificate(clientname);
		
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "Client is deleted.");
		return null;
	}
}
