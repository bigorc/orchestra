package org.oc.orchestra.client;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

@SuppressWarnings("deprecation")
public class HttpCommand {
	private HttpRequestBase request;
	private HttpClient httpclient;
	
	public HttpCommand(HttpRequestBase request) {
		this.request = request;
	}

	private HttpClient getHttpClient() {
		HttpClient client = new DefaultHttpClient();
		SSLSocketFactory socketFactory = SSLSocketFactory.getSystemSocketFactory();

		Scheme sch = new Scheme("https", socketFactory, 8183);
        client.getConnectionManager().getSchemeRegistry().register(sch);
        return client;
	}
	
	public HttpResponse execute() {
		httpclient = getHttpClient();
		try {
			return httpclient.execute(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
