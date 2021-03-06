package org.orchestra.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.UUID;

import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.orchestra.auth.Constants;
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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Apikey extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Apikey.class);
	@Get
	public Representation getApikey() throws JSONException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		String username = (String) getRequest().getAttributes().get("username");
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("username:" + username);
		logger.debug("clientname:" + clientname);
		DBCollection coll = ServerAuthHelper.getUserCollection();
		BasicDBObject query = new BasicDBObject("username", username)
			.append("clientname", clientname);
		DBObject dbo = coll.findOne(query);
    	if(dbo == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
    	String apikey = (String) dbo.get("apikey");
    	String secret = (String) dbo.get("secret");
		
		JSONObject json = new JSONObject();
		json.put("apikey", apikey);
		json.put("secret", secret);
		
		getResponse().setStatus(Status.SUCCESS_OK);
		return new JsonRepresentation(json);
	}

	@Post
	public Representation createApikey() throws JSONException, UnrecoverableKeyException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		String username = (String) getRequest().getAttributes().get("username");
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("username:" + username);
		logger.debug("clientname:" + clientname);
		
		DBCollection coll = ServerAuthHelper.getUserCollection();
		BasicDBObject query = new BasicDBObject("username", username)
			.append("clientname", clientname);
		DBCursor cursor = coll.find(query);
		
		if(cursor.count() == 0) {
			JSONObject json = generateApikey();
			getResponse().setStatus(Status.SUCCESS_CREATED);
			return new JsonRepresentation(json);
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Apikey already exists.");
			return new StringRepresentation("Apikey already exists.");
		}
	}

	@Put
	public Representation updateApikey() throws UnrecoverableKeyException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, JSONException, IOException {
		JSONObject json = generateApikey();
		getResponse().setStatus(Status.SUCCESS_OK);
		return new JsonRepresentation(json);
	}
	
	@Delete
	public Representation deleteApikey() {
		String username = (String) getRequest().getAttributes().get("username");
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("username:" + username);
		logger.debug("clientname:" + clientname);
		
		DBCollection coll = ServerAuthHelper.getUserCollection();
		BasicDBObject query = new BasicDBObject("username", username)
			.append("clientname", clientname);
		coll.remove(query);
		getResponse().setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("Apikey is deleted.");
		
	}
	
	private JSONObject generateApikey() throws JSONException, UnrecoverableKeyException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		String username = (String) getRequest().getAttributes().get("username");
		String clientname = (String) getRequest().getAttributes().get("clientname");
		logger.debug("username:" + username);
		logger.debug("clientname:" + clientname);
		
		DBCollection coll = ServerAuthHelper.getUserCollection();
		BasicDBObject query = new BasicDBObject("username", username)
			.append("clientname", clientname);
		DBCursor cursor = coll.find(query);
		
		String apikey = UUID.randomUUID().toString() + UUID.randomUUID().toString();
		String secret = new SecureRandomNumberGenerator().nextBytes(64).toBase64();
		if(cursor.count() == 0) {
			BasicDBObject doc = new BasicDBObject("username", username)
				.append("clientname", clientname)
				.append("apikey", apikey)
				.append("secret", secret)
				.append("timestamp", new DateTime().toString(Constants.TIMESTAMP_FORMAT));
			coll.insert(doc);
		} else {
			BasicDBObject criteria = new BasicDBObject("username", username)
				.append("clientname", clientname);
			BasicDBObject action = new BasicDBObject("$set", new BasicDBObject()
				.append("apikey", apikey)
				.append("secret", secret)
			.append("timestamp", new DateTime().toString(Constants.TIMESTAMP_FORMAT)));
			coll.update(criteria, action);
		}
		JSONObject json = new JSONObject();
		json.put("apikey", apikey);
		json.put("secret", secret);
		return json;
	}

}
