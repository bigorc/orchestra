package org.oc.orchestra.auth;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.restlet.Request;

public class RequestToken extends UsernamePasswordToken {
	private Request request;

	public Request getRequest() {
		return request;
	}

	public RequestToken(String username, Request request) {
		super(username, "");
		this.request = request;
	}
}
