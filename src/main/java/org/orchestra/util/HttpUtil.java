package org.orchestra.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.orchestra.auth.Constants;

public class HttpUtil {
	
	public static int put(String url) {
		HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPut httpPut = new HttpPut(url);
        HttpResponse response = null;
		try {
			response = httpClient.execute(httpPut);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        System.out.println(response.getStatusLine());
        return response.getStatusLine().getStatusCode();
	}
	
	public static String canonicalizeQueryString(String queryString) {
		if(queryString == null || queryString.length() == 0) return "";
		String[] params = queryString.split("&");
		StringBuilder sb = new StringBuilder();
		
		for(String param : params) {
			String[] kv = param.split("=");
			if(kv[0].equals(Constants.PARAMETER_SIGNATURE)) continue;
			String key = encodeUrl(kv[0], false);
			String value = encodeUrl(kv[1], false);
			
			if (sb.length() > 0) {
				sb.append('&');
			}
			
			sb.append(key).append("=").append(value);
		}
		
		return sb.toString();
	}

	public static String canonicalURI(String uri) throws UnsupportedEncodingException {
		if (uri == null || uri.length() == 0) {
			return "/";
		} else {
			return encodeUrl(uri, true);
		}
	}
	
	public static String encodeUrl(String value, boolean path) {
		if (value == null || value.equals("")) {
			return "";
		}
		
		String encoded;
		
		try {
			encoded = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("Unable to UTF-8 encode url string component [" + value + "]", ex);
		}
		
		encoded = encoded.replace("+", "%20")
				.replace("*", "%2A")
				.replace("%7E", "~"); //yes, this is reversed (compared to the 2 above it) intentionally
		
		if (path) {
			encoded = encoded.replace("%2F", "/");
		}
		
		return encoded;
	}

	
}
