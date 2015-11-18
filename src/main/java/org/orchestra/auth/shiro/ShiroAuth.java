package org.orchestra.auth.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.orchestra.auth.Constants;
import org.orchestra.rest.Apikey;
import org.orchestra.rest.ServerAuthHelper;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class ShiroAuth {
	private final Logger logger = LoggerFactory.getLogger(ShiroAuth.class);
	private Subject user;
	
	public ShiroAuth(String username, String password) {
		this.user = SecurityUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken(username, password);
		user.login(token);
	}
	
	public ShiroAuth(Request request) {
		String apikey = request.getResourceRef().getQueryAsForm()
				.getFirstValue(Constants.PARAMETER_APIKEY);
		DBCollection coll = ServerAuthHelper.getUserCollection();
		DBObject userObj = coll.findOne(new BasicDBObject("apikey", apikey));
		String username = null;
		if(userObj != null) username = (String) userObj.get("username");
		
		RequestToken token = new RequestToken(username, request);
		logger.debug("Authenticating user:" + username);
		this.user = SecurityUtils.getSubject();
		user.login(token);
	}
	
	public boolean isAuthenticated() {
		return user.isAuthenticated();
	}
	
	public boolean hasRole(String role) {
		return user.hasRole(role);
	}
	
	public boolean hasPermission(String permission) {
		return user.isPermitted(permission);
	}
}
