package org.orchestra.auth.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.restlet.Request;

public class RequestToken extends UsernamePasswordToken {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8351989333846006203L;
	private Request request;

	public Request getRequest() {
		return request;
	}

	public RequestToken(String username, Request request) {
		super(username, "");
		this.request = request;
	}
}
