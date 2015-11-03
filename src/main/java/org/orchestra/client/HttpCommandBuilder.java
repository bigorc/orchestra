package org.orchestra.client;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.codec.Base64;
import org.joda.time.DateTime;
import org.orchestra.auth.Constants;

public class HttpCommandBuilder {
	private String username;
	private String password;
	private HttpRequestBase request;
	private URIBuilder uriBuilder;
	private Map<String, Object> parameters;
	private Map<String, String> headers;
	private String target;
	private boolean needAuthHeader;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isNeedAuthHeader() {
		return needAuthHeader;
	}

	public HttpCommandBuilder setNeedAuthHeader(boolean needAuthHeader) {
		this.needAuthHeader = needAuthHeader;
		return this;
	}

	public HttpCommandBuilder(String username, String password) {
		this.username = username;
		this.password = password;
		uriBuilder = new URIBuilder();
		parameters = new HashMap<String, Object>();
		headers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	}

	public HttpCommand build() {
		
		try {
			if(needAuthHeader) {
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8));
				String authHeader = "Basic " + new String(encodedAuth);
				request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
			} else {
				ClientAuthHelper helper = new ClientAuthHelper(username, password);
				String apikey = helper.getApikey();
				setParameter(Constants.PARAMETER_APIKEY, apikey);
				String nonce = UUID.randomUUID().toString();
				setParameter(Constants.PARAMETER_NONCE, nonce);
				DateTime date = new DateTime();
				String timestamp = date.toString(Constants.TIMESTAMP_FORMAT);
				setParameter(Constants.PARAMETER_TIMESTAMP, timestamp);
				String signedHeaders = helper.getSignedHeadersString(this);
				addHeader(Constants.SIGNED_HEADERS, signedHeaders);
				String signature;
				try {
					signature = helper.sign(this);
					setParameter("signature", signature);
				} catch (SignatureException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			request.setURI(uriBuilder.build());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return new HttpCommand(request);
	}

	public HttpCommandBuilder setAction(String method) {
		if(method.equals("create")) {
			request = new HttpPost();
		}
		if(method.equals("update")) {
			request = new HttpPut();
		}
		if(method.equals("read")) {
			request = new HttpGet();
		}
		if(method.equals("delete")) {
			request = new HttpDelete();
		}
		return this;
	}

	public HttpCommandBuilder setMethod(String method) {
		if(method.equalsIgnoreCase("post")) {
			request = new HttpPost();
		}
		if(method.equalsIgnoreCase("put")) {
			request = new HttpPut();
		}
		if(method.equalsIgnoreCase("get")) {
			request = new HttpGet();
		}
		if(method.equalsIgnoreCase("delete")) {
			request = new HttpDelete();
		}
		return this;
	}

	public HttpCommandBuilder setTarget(String target) {
		this.target = target;
		uriBuilder.setPath("/" + target);
		return this;
	}
	
	public HttpCommandBuilder setScheme(String scheme) {
		this.uriBuilder.setScheme(scheme);
		return this;
	}
	
	public HttpCommandBuilder setHost(String host) {
		this.uriBuilder.setHost(host);
		return this;
	}
	
	public HttpCommandBuilder setPort(int port) {
		this.uriBuilder.setPort(port);
		return this;
	}

	public HttpCommandBuilder setParameter(String name, String value) {
		uriBuilder.setParameter(name, value);
		parameters.put(name, value);
		return this;
	}

	public HttpCommandBuilder addParameter(String name, String value) {
		uriBuilder.addParameter(name, value);
		if(parameters.get(name) == null) {
			parameters.put(name, value);
		} else {
			if(parameters.get(name) instanceof List) {
				((List)parameters.get(name)).add(value);
			} else {
				List<String> list = new ArrayList<String>();
				list.add((String) parameters.get(name));
				list.add(value);
				parameters.put(name, list);
			}
		}
		return this;
	}
	public HttpCommandBuilder setEntity(HttpEntity entity) {
		if(request instanceof HttpPost) {
			((HttpPost)request).setEntity(entity);
			return this;
		} else if(request instanceof HttpPut) {
			((HttpPut)request).setEntity(entity);
			return this;
		} else {
			throw new RuntimeException("Can't set entity to Get or Delete request");
		}
	}

	public HttpCommandBuilder addHeader(String name, String value) {
		request.addHeader(name, value);
		headers.put(name, value);
		return this;
	}

	public HttpCommandBuilder addPathParameter(String parameter) {
		uriBuilder.setPath(uriBuilder.getPath() + "/" + parameter);
		return this;
	}
	
	public String getHeader(String name) {
		return headers.get(name);
	}
	
	public String getParameter(String name) {
		return (String) parameters.get(name);
	}

	public String getParameter(String name, int index) {
		return (String) ((List)parameters.get(name)).get(index); 
	}
	public String getMethod() {
		return request.getMethod();
	}

	public String getPath() {
		return uriBuilder.getPath();
	}

	public String getQueryString() {
		StringBuilder sb = new StringBuilder();
		for( NameValuePair pair : uriBuilder.getQueryParams()) {
			String name = pair.getName();
			String value = pair.getValue();
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(name).append("=").append(value);
		}
		return sb.toString();
	}

	public Map<String, String> getAllHeaders() {
		return headers;
	}

	public HttpEntity getEntity() {
		HttpEntity entity = null;
		if(request instanceof HttpPost) {
			entity = ((HttpPost)request).getEntity();
		} else if (request instanceof HttpPut) {
			entity = ((HttpPut)request).getEntity();
		}
		return entity;
	}

	public HttpRequestBase getRequest() {
		return request;
	}
}
