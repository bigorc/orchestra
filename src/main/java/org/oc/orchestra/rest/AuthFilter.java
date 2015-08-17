package org.oc.orchestra.rest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.Factory;
import org.oc.orchestra.auth.ShiroAuth;
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
		System.out.println("AuthFilter was invoked.");
		
		String path = request.getResourceRef().getPath();
		Factory<org.apache.shiro.mgt.SecurityManager> factory = 
				new IniSecurityManagerFactory("shiro.ini");
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
		
		if(path.startsWith("/apikey") || path.startsWith("/client")) {
			String[] name_pass = getUserPass(request);
			String username = name_pass[0];
			String password = name_pass[1];
			System.out.println("username:" + username + ";password:" + password);
			
			ShiroAuth shiro = new ShiroAuth(username, password);
			String user = path.split("/")[2];
			System.out.println(user);
			if(!shiro.isAuthenticated()) {
				System.out.println("user is not authenticated!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "You are not authenticated");
			} else if (!shiro.hasRole("admin") && !shiro.hasPermission("apikey:" + user + ":*")) {
				System.out.println("user is not authorized!");
				response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You are not authorized");
			}
		} else {
			ShiroAuth shiro = new ShiroAuth(request);
			if(!shiro.isAuthenticated()) {
				System.out.println("user is not authenticated!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "You are not authenticated");
			}
		}
		return result;
	}

	@Override
	protected void afterHandle(Request request, Response response) {
		logger.info("client authenticated:" + request.getClientInfo().isAuthenticated());
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
