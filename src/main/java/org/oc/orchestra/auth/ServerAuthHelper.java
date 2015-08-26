package org.oc.orchestra.auth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.oc.orchestra.sm.StateMachine;
import org.oc.util.CipherUtil;
import org.oc.util.HttpUtil;
import org.oc.util.SpringUtil;
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
	private static final int NONCE_EXPIRATION_PERIOD = 15;
	private final Logger logger = LoggerFactory.getLogger(ServerAuthHelper.class);
	private static final int NONCE_DELETION_PERIOD = 1;
	private static final int REQUEST_TIMEOUT = 5;
	public static final String LAST_DELETION_TIME = "last_delete";
	public static final String MINUTESTAMPS = "minute_stamps";
	private static Map<String, String> cache;
	private Request request;
	private static DB db;
	private static DBCollection debugColl;
	
	public ServerAuthHelper(Request request) {
		this.request = request;
		if(db == null) db = getDB();
	}

	public static DB getDB() {
		MongoClient mongoClient = (MongoClient) SpringUtil.getBean("mongoClient");
		DB db = mongoClient.getDB( "orchestra" );
		return db;
	}

	private String getParameter(String name) {
		Form form = request.getResourceRef().getQueryAsForm();
		return form.getFirstValue(name);
	}
	
	public String getSignature() throws SignatureException, UnrecoverableKeyException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		BasicDBObject query = new BasicDBObject("apikey", getParameter(Constants.PARAMETER_APIKEY));
		DBCollection coll = db.getCollection("user");
		DBObject dbo = coll.findOne(query);
		System.out.println(dbo);
    	String secret = (String) dbo.get("secret");
		String canonicalRequestHashHex = CipherUtil.toHex(CipherUtil.hash(getCanonicalRequest()));
		
		String timestamp = getParameter(Constants.PARAMETER_TIMESTAMP);
		String nonce = getParameter(Constants.PARAMETER_NONCE);
		String stringToSign =
				Constants.SIGNATURE_ALGORITHM+ Constants.NEW_LINE +
				timestamp  + Constants.NEW_LINE +
				canonicalRequestHashHex;
		System.out.println(stringToSign);
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Constants.TIMESTAMP_FORMAT);
		DateTime date = formatter.parseDateTime(timestamp);
		String dateStamp = date .toString(Constants.DATE_FORMAT);

		byte[] kDate = CipherUtil.sign(dateStamp, secret);
		byte[] kSigning = CipherUtil.sign(nonce  , kDate);
		byte[] signature = CipherUtil.sign(stringToSign, kSigning);
		String signatureHex = CipherUtil.toHex(signature);
		DBObject matcher = debugColl.findOne();
		System.out.println(matcher.get("secret") + "|" + secret);
		System.out.println("Does the secret match?" + matcher.get("secret").equals(secret));
		System.out.println(matcher.get("stringToSign") + "|" + stringToSign);
		System.out.println("Does the stringToSign match?" + matcher.get("stringToSign").equals(stringToSign));
		System.out.println(matcher.get("kDate") + "|" + new String(kDate, StandardCharsets.UTF_8));
		System.out.println("Does the kDate match?" + matcher.get("kDate").equals(kDate.toString()));
		System.out.println(matcher.get("kSigning") + "|" + kSigning);
		System.out.println("Does the kSigning match?" + matcher.get("kSigning").equals(kSigning.toString()));
		System.out.println(matcher.get("signature") + "|" + signature);
		System.out.println("Does the signature match?" + matcher.get("signature").equals(signatureHex));
		
		System.out.println(signatureHex);
		
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
		if(date.plusMinutes(REQUEST_TIMEOUT).isBeforeNow() || date.minusMinutes(5).isAfterNow()) 
			return false;
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
		    	System.out.println(dbo);
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
		
		
		if(date.plusMinutes(NONCE_DELETION_PERIOD).isBeforeNow()) {
			logger.info("The last elimination of outdated nonces is 1 minutes ago, need to delete again.");
			DBCollection minColl = db.getCollection(MINUTESTAMPS);
			DateTimeFormatter minFormatter = DateTimeFormat.forPattern(Constants.MINUTES_TIME_FORMAT);
			String expiration_time = current.minusMinutes(NONCE_EXPIRATION_PERIOD).toString(minFormatter);
			BasicDBObject query = new BasicDBObject("minutestamp", 
					new BasicDBObject("$lt", expiration_time));
			DBCursor cursor = minColl.find(query);
			try {
			    while(cursor.hasNext()) {
			    	DBObject dbo = cursor.next();
			    	System.out.println(dbo);
			    	db.getCollection((String) dbo.get("minutestamp")).remove(new BasicDBObject());
			    }
			} finally {
			    cursor.close();
			}
			System.out.println(expiration_time);
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
		String requestPayloadHashHex = CipherUtil.toHex(CipherUtil.hash(getRequestPayload()));
		
		String canonicalRequest =
				method + Constants.NEW_LINE +
				canonicalURI + Constants.NEW_LINE +
				canonicalQueryString + Constants.NEW_LINE +
				canonicalHeadersString + Constants.NEW_LINE +
				signedHeadersString + Constants.NEW_LINE +
				requestPayloadHashHex;
		
		debugColl = db.getCollection("debug");
		DBObject matcher = debugColl.findOne();
		System.out.println(matcher.get("uri") + "|" + canonicalURI);
		System.out.println("Does the uri match?" + matcher.get("uri").equals(canonicalURI));
		System.out.println(matcher.get("query") + "|" + canonicalQueryString);
		System.out.println("Does the query string match?" + matcher.get("query").equals(canonicalQueryString));
		System.out.println(matcher.get("cheader") + "|" + canonicalHeadersString);
		System.out.println("Does the canonical header match?" + matcher.get("cheader").equals(canonicalHeadersString));
		System.out.println(matcher.get("sheader") + "|" + signedHeadersString);
		System.out.println("Does the signed header match?" + matcher.get("sheader").equals(signedHeadersString));
		System.out.println(matcher.get("payload") + "|" + requestPayloadHashHex);
		System.out.println("Does the payload match?" + matcher.get("payload").equals(requestPayloadHashHex));
		
		
		System.out.println(canonicalRequest);
		return canonicalRequest;
	}

	private String getRequestPayload() {
//		String string = request.getEntity().getDigest().toString();
//		if(string == null) string = "";
//		System.out.println(string);
		return "";
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
		System.out.println(queryString);
		return HttpUtil.canonicalizeQueryString(queryString);
	}

}
