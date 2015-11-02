package org.oc.orchestra.auth;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.oc.orchestra.rest.Apikey;
import org.oc.util.SpringUtil;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class ShiroAuth {
	private Subject user;
	
	public ShiroAuth(String username, String password) {
		this.user = SecurityUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken(username, password);
		user.login(token);
	}
	
	public ShiroAuth(Request request) {
		String apikey = request.getResourceRef().getQueryAsForm()
				.getFirstValue(Constants.PARAMETER_APIKEY);
		DBCollection coll = Apikey.getStore();
		DBObject userObj = coll.findOne(new BasicDBObject("apikey", apikey));
		String username = null;
		if(userObj != null) username = (String) userObj.get("username");
		
		RequestToken token = new RequestToken(username, request);
		
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
