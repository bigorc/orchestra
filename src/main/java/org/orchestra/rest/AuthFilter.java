package org.orchestra.rest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.Factory;
import org.orchestra.auth.shiro.ShiroAuth;
import org.orchestra.util.SpringUtil;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.routing.Filter;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthFilter extends Filter {
	private final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
	
	@Override
	protected int beforeHandle (Request request, Response response) {
		int result = CONTINUE;
		
		String path = request.getResourceRef().getPath();
		
		@SuppressWarnings("unchecked")
		Factory<org.apache.shiro.mgt.SecurityManager> factory = 
				(Factory<SecurityManager>) SpringUtil.getBean("securityManager");

		SecurityManager securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);
		List<java.security.cert.Certificate> certs = request.getClientInfo().getCertificates();
		if(certs.size() != 0) {
			for(Certificate cert : certs) {
				X509Certificate x509 = (X509Certificate) cert;
				logger.info("X509 DN:" + x509.getSubjectDN().getName());
				logger.info("Client is authenticated");
			}
			request.getClientInfo().setAuthenticated(true);
		} else {
			logger.info("client is not authenticatd");
		}
		
		if(path.startsWith("/apikey") || path.startsWith("/client") || path.startsWith("/zkauth")) {
			String[] name_pass = getUserPass(request);
			String username = name_pass[0];
			String password = name_pass[1];
			
			ShiroAuth shiro = new ShiroAuth(username, password);
			if(!shiro.isAuthenticated()) {
				logger.warn(username + " is trying to access " + path + " and  is not authenticated!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "You are not authenticated");
			} else if (path.startsWith("/apikey")) {
				String user = path.split("/")[2];
				logger.debug("checking permission to access apikey of " + user);
				if(!shiro.hasRole("admin") 
						&& !shiro.hasPermission("apikey:" + user + ":*") 
						&& !username.equals(user)) {
					logger.warn(username + " is trying to access " + path + " and  is not authenticated!");
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Unauthorized operation");
				}
			} else if(!shiro.hasRole("admin")) {
				logger.warn(username + " is trying to access " + path + " and  is not authenticated!");
				response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Unauthorized operation");
			}
		} else {
			ShiroAuth shiro = new ShiroAuth(request);
			if(!request.getClientInfo().isAuthenticated()) {
				logger.info("Unauthorized client");
				response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Unauthorized client");
			}
			
			if(!shiro.isAuthenticated()) {
				logger.info("user is not authenticated!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Not authenticated");
			}
		}
		return result;
	}

	public static String[] getUserPass(Request request) {
		Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
		String authHeader = headers.getFirstValue("Authorization");
		String encodedAuth = authHeader.split(" ")[1];
		String auth = Base64.decodeToString(encodedAuth);
		String[] name_pass = auth.split(":");
		return name_pass;
	}

	
}
