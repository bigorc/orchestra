package org.orchestra.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.orchestra.auth.Constants;
import org.orchestra.util.CipherUtil;
import org.orchestra.util.HttpUtil;
import org.orchestra.util.SpringUtil;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.engine.header.Header;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class ServerAuthHelper {
	private final Logger logger = LoggerFactory.getLogger(ServerAuthHelper.class);
	public static final String LAST_DELETION_TIME = "last_delete";
	public static final String MINUTESTAMPS = "minute_stamps";
	private Request request;
	private static DB db;
	private static DBCollection userColl;
	
	public ServerAuthHelper(Request request) {
		this.request = request;
		if(db == null) db = getDB();
	}

	public static DB getDB() {
		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient(Server.getProperty("mongodb.host"), 
					Integer.valueOf(Server.getProperty("mongodb.port")));
		} catch (NumberFormatException | UnknownHostException e) {
			throw new RuntimeException(e);
		}
		DB db = mongoClient.getDB( "orchestra" );
		boolean auth = db.authenticate(Server.getProperty("mongodb.user"), 
				Server.getProperty("mongodb.password").toCharArray());
		if(!auth) {
			throw new RuntimeException("Mongodb authentication failed for user " + Server.getProperty("mongodb.user"));
		}
		return db;
	}

	public static DBCollection getUserCollection() {
		if(db == null) db = getDB();
		if(userColl == null) userColl = db.getCollection("user");
		return userColl;
	}

	private String getParameter(String name) {
		Form form = request.getResourceRef().getQueryAsForm();
		return form.getFirstValue(name);
	}
	
	public String getSignature() throws SignatureException, UnrecoverableKeyException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		BasicDBObject query = new BasicDBObject("apikey", getParameter(Constants.PARAMETER_APIKEY));
		DBCollection coll = db.getCollection("user");
		DBObject dbo = coll.findOne(query);
    	String secret = (String) dbo.get("secret");
		String canonicalRequestHashHex = CipherUtil.toHex(CipherUtil.hash(getCanonicalRequest()));
		
		String timestamp = getParameter(Constants.PARAMETER_TIMESTAMP);
		String nonce = getParameter(Constants.PARAMETER_NONCE);
		String stringToSign =
				Constants.SIGNATURE_ALGORITHM+ Constants.NEW_LINE +
				timestamp  + Constants.NEW_LINE +
				canonicalRequestHashHex;
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Constants.TIMESTAMP_FORMAT);
		DateTime date = formatter.parseDateTime(timestamp);
		String dateStamp = date .toString(Constants.DATE_FORMAT);

		byte[] kDate = CipherUtil.sign(dateStamp, secret);
		byte[] kSigning = CipherUtil.sign(nonce  , kDate);
		byte[] signature = CipherUtil.sign(stringToSign, kSigning);
		String signatureHex = CipherUtil.toHex(signature);
	
		return signatureHex;
	}

	public boolean validate() throws SignatureException, UnrecoverableKeyException, 
		KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, 
			CertificateException, IOException {
		if(!validateTimestamp(getParameter(Constants.PARAMETER_TIMESTAMP)))
			return false;
		if(!validateNonce(getParameter(Constants.PARAMETER_TIMESTAMP), 
						getParameter(Constants.PARAMETER_NONCE)))
			return false;
		return getParameter(Constants.PARAMETER_SIGNATURE).equals(getSignature());
	}


	public boolean validateTimestamp(String timestamp) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Constants.TIMESTAMP_FORMAT);
		DateTime date = formatter.parseDateTime(timestamp);
		Integer timeout = Integer.valueOf(Server.getProperty("synctime.limit"));
		if(date.plusMinutes(timeout).isBeforeNow()
				|| date.minusMinutes(timeout).isAfterNow()) {
			logger.info("Authentication failed because request time deviation exceed limit of " 
				+ timeout + "minutes");
			return false;
		}
			
		return true;
	}

	public boolean validateNonce(String timestamp, String nonce) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Constants.TIMESTAMP_FORMAT);
		DateTime date = formatter.parseDateTime(timestamp);
		timestamp = date.toString(Constants.SECONDS_TIME_FORMAT);
		String minuteStamp = date.toString(Constants.MINUTES_TIME_FORMAT);
		DBCollection msColl = db.getCollection(MINUTESTAMPS);
		DBCursor cursor = msColl.find();
		try {
		    while(cursor.hasNext()) {
		    	DBObject dbo = cursor.next();
		    	//shard nonces by minutestamp
		    	DBCollection coll = db.getCollection((String) dbo.get("minutestamp"));
		    	if(coll.find(new BasicDBObject("nonce", nonce)).count() != 0) {
		    		return false;
		    	}
		    }
		} finally {
		    cursor.close();
		}
		BasicDBObject minQuery = new BasicDBObject("minutestamp", minuteStamp);
		if(msColl.find(minQuery).count() == 0) {
			msColl.insert(minQuery);
		}
		DBCollection nonceColl = db.getCollection(minuteStamp);
		nonceColl.insert(new BasicDBObject("nonce", nonce));
		delete_expired_nonces();
		return true;
	}

	public void delete_expired_nonces() {
		DateTime current= new DateTime();
		//LAST_DELETION_TIME collection should have at most one document
		DBCollection coll = db.getCollection(LAST_DELETION_TIME);
		DBObject ldObj = coll.findOne();
		String last_delete = current.toString(Constants.SECONDS_TIME_FORMAT);
		if(ldObj == null) {
			coll.insert(new BasicDBObject(LAST_DELETION_TIME, 
					last_delete));
		} else {
			last_delete = (String) ldObj.get(LAST_DELETION_TIME);
		}
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Constants.SECONDS_TIME_FORMAT);
		DateTime date = formatter.parseDateTime(last_delete);
		
		
		if(date.plusMinutes(Integer.valueOf(Server.getProperty("nonce.deletion.period"))).isBeforeNow()) {
			logger.info("Deleting outdated nonces.");
			DBCollection minColl = db.getCollection(MINUTESTAMPS);
			DateTimeFormatter minFormatter = DateTimeFormat.forPattern(Constants.MINUTES_TIME_FORMAT);
			String expiration_time = current.minusMinutes(Integer.valueOf(
					Server.getProperty("nonce.expiration.period"))).toString(minFormatter);
			BasicDBObject query = new BasicDBObject("minutestamp", 
					new BasicDBObject("$lt", expiration_time));
			DBCursor cursor = minColl.find(query);
			try {
			    while(cursor.hasNext()) {
			    	DBObject dbo = cursor.next();
			    	db.getCollection((String) dbo.get("minutestamp")).remove(new BasicDBObject());
			    }
			} finally {
			    cursor.close();
			}
			minColl.remove(query);
			coll.remove(new BasicDBObject());
			coll.insert(new BasicDBObject(LAST_DELETION_TIME, current.toString(formatter)));
		}
	}

	public String getCanonicalRequest() throws SignatureException, UnsupportedEncodingException {
		String method = request.getMethod().toString();
		String canonicalURI  = HttpUtil.canonicalURI(request.getResourceRef().getPath());
		String canonicalQueryString = canonicalizeQueryString();
		String canonicalHeadersString = canonicalizeHeadersString();
		String signedHeadersString = getSignedHeadersString();

		String canonicalRequest =
				method + Constants.NEW_LINE +
				canonicalURI + Constants.NEW_LINE +
				canonicalQueryString + Constants.NEW_LINE +
				canonicalHeadersString + Constants.NEW_LINE +
				signedHeadersString;
		return canonicalRequest;
	}

	private String getSignedHeadersString() {
		Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
		return headers.getFirstValue(Constants.SIGNED_HEADERS);
	}

	private String canonicalizeHeadersString() {
		Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
		List<String> sortedHeaders = new ArrayList<String>();
		List<String> signedHeaders = Arrays.asList(getSignedHeadersString().split(";"));
		for(String hn : headers.getNames()) {
			if(signedHeaders.contains(hn.toLowerCase())) sortedHeaders.add(hn);
		}
		Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

		StringBuilder buffer = new StringBuilder();
		for (String header : sortedHeaders) {
			buffer.append(header.toLowerCase()).append(":");
			String values = headers.getValues(header);
			buffer.append(values.trim());
			buffer.append(Constants.NEW_LINE);
		}
		
		return buffer.toString();
	}
	
	private String canonicalizeQueryString() {
		Form form = request.getResourceRef().getQueryAsForm();
		String queryString = form.getQueryString();
		queryString = URLDecoder.decode(queryString);
		return HttpUtil.canonicalizeQueryString(queryString);
	}

}
