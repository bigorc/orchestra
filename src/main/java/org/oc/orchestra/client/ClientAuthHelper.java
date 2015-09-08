package org.oc.orchestra.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.auth.Constants;
import org.oc.orchestra.auth.ServerAuthHelper;
import org.oc.util.CipherUtil;
import org.oc.util.HttpUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class ClientAuthHelper {
	private static Map<String, String> cache = new HashMap<String, String>();
	private static String host = "orchestra";
	private static int port = 8183;
//	private static BasicDBObject matcher;
	private String username;
	private String password;
	private String filename;
	
	public ClientAuthHelper(String username, String password) {
		this.username = username;
		this.password = password;
		this.filename = new Md5Hash(username) + ".apikey";
	}
	
	public String getApikey() {
		String apikey = cache.get(username);
		if(apikey != null) return apikey;
		apikey = loadApikeyFromFile();
		if(apikey != null) return apikey;
		apikey = getApikeyFromServer();
		return apikey;
	}

	private String getApikeyFromServer() {
		HttpCommandBuilder builder = new HttpCommandBuilder(username, password);
		HttpCommand command = builder.setScheme("https")
			.setNeedAuthHeader(true)
			.setHost(host)
			.setPort(port)
			.setAction("update")
			.setTarget("apikey")
			.addPathParameter(username)
			.addPathParameter(Client.getName())
			.build();
		HttpResponse response = command.execute();
		if(200 != response.getStatusLine().getStatusCode()) {
			throw new RuntimeException("Unable to get apikey from server.");
		}
		String apikey = saveApikeyToFile(response);
		
		return apikey;
	}
	
	public void removeApikeyFile() {
		File file = new File(filename);
		if(file.exists()) file.delete();
	}

	public String saveApikeyToFile(HttpResponse response) {
		Reader in = null;
		try {
			in = new InputStreamReader(response.getEntity().getContent());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
		JSONObject json = (JSONObject) JSONValue.parse(in);
		String apikey = null;
		if(json != null) {
			apikey = (String) json.get("apikey");
			String secret = (String) json.get("secret");
			cache.put(username, apikey);
			cache.put(apikey, secret);
			String jsonString = json.toJSONString();
			System.out.println(jsonString);
			OutputStream out = null;
			try {
				out = new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			CipherUtil.encrypt(jsonString , out , password);
		}
		return apikey;
	}

	private String loadApikeyFromFile() {
		File file = new File(filename);
		InputStream in = null;
		if(!file.exists()) return null;
		
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String jsonString = CipherUtil.decrypt(in, password);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		String apikey = (String) json.get("apikey");
		String secret = (String) json.get("secret");
		cache.put(username, apikey);
		cache.put(apikey, secret);
		return apikey;
	}
	
	public String sign(HttpCommandBuilder request) throws SignatureException, UnsupportedEncodingException {
		String secret = cache.get(getApikey());
		String canonicalRequestHashHex = CipherUtil.toHex(CipherUtil.hash(getCanonicalRequest(request)));
		
		String timestamp = (String) request.getParameter("timestamp");
		String nonce = (String) request.getParameter("nonce");
		String stringToSign =
				Constants.SIGNATURE_ALGORITHM + Constants.NEW_LINE +
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
//		matcher.put("secret", secret);
//		matcher.put("stringToSign", stringToSign);
//		matcher.put("kDate", new String(kDate, StandardCharsets.UTF_8));
//		matcher.put("kSigning", kSigning.toString());
//		matcher.put("signature", signatureHex);
//		DBCollection coll = ServerAuthHelper.getDB().getCollection("debug");
//		coll.remove(new BasicDBObject());
//		coll.insert(matcher);
		System.out.println(signatureHex);
		return signatureHex;
	}

	public static String getCanonicalRequest(HttpCommandBuilder request) throws UnsupportedEncodingException, SignatureException {
		String method = request.getMethod();
		String canonicalURI  = HttpUtil.canonicalURI(request.getPath());
		String canonicalQueryString = canonicalizeQueryString(request);
		String canonicalHeadersString = canonicalizeHeadersString(request);
		String signedHeadersString = getSignedHeadersString(request);
//		String requestPayloadHashHex = CipherUtil.toHex(
//				CipherUtil.hash(getRequestPayload(request)));
		
//		matcher = new BasicDBObject();
//		matcher.put("uri", canonicalURI);
//		matcher.put("query", canonicalQueryString);
//		matcher.put("cheader", canonicalHeadersString);
//		matcher.put("sheader", signedHeadersString);
//		matcher.put("payload", requestPayloadHashHex);
		
		String canonicalRequest =
				method + Constants.NEW_LINE +
				canonicalURI + Constants.NEW_LINE +
				canonicalQueryString + Constants.NEW_LINE +
				canonicalHeadersString + Constants.NEW_LINE +
				signedHeadersString;
//				+ Constants.NEW_LINE + requestPayloadHashHex;
		System.out.println(canonicalRequest);
		return canonicalRequest;
	}
	
	private static String canonicalizeHeadersString(HttpCommandBuilder request) {
		Map<String, String> headers = request.getAllHeaders();
		StringBuilder buffer = new StringBuilder();
		for( Entry<String, String> header : headers.entrySet()) {
			if(header.getKey().equalsIgnoreCase(Constants.SIGNED_HEADERS)) continue;
			buffer.append(header.getKey().toLowerCase()).append(":");
			String values = header.getValue();
			buffer.append(values.trim());
			buffer.append(Constants.NEW_LINE);
		}
		System.out.println(buffer.toString());
		return buffer.toString();
	}

	private static String canonicalizeQueryString(HttpCommandBuilder request) {
		String queryString = request.getQueryString();
		System.out.println(queryString);
		return HttpUtil.canonicalizeQueryString(queryString);
	}

	public static String getSignedHeadersString(HttpCommandBuilder request) {
		Map<String, String> headers = request.getAllHeaders();
		StringBuilder buffer = new StringBuilder();
		for(Entry<String, String> header : headers.entrySet()) {
			if(header.getKey().equalsIgnoreCase(Constants.SIGNED_HEADERS)) continue;
			if (buffer.length() > 0) buffer.append(";");
			buffer.append(header.getKey().toLowerCase());
		}
		return buffer.toString();
	}
//
//	private static String getRequestPayload(HttpCommandBuilder request) {
//		InputStream content = null;
//		String string = null;
//		try {
//			HttpEntity entity = request.getEntity();
//			if(entity != null) 
//				content = entity.getContent();
//			if (content == null) return "";
//			string = IOUtils.toString(content, StandardCharsets.UTF_8);
//		} catch (Exception e) {
//			return "";
//		}
//		
//		System.out.println(string);
//		return string;
//	}

	private static class HeaderComparator implements Comparator<org.apache.http.Header> {
		@Override
		public int compare(org.apache.http.Header o1,
				org.apache.http.Header o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}
			
}
