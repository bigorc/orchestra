package org.oc.orchestra.rest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.UserDao;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class User extends ServerResource {
	@Override
	protected Representation get() {
		System.out.println("The GET method of user resource was invoked.");
		UserDao userDao = (UserDao) SpringUtil.getBean("userDao");
		
		String userName = (String) getRequest().getAttributes().get("username");
		org.oc.orchestra.dao.User user = userDao.getUser(userName );
		JSONObject jo = new JSONObject();
		try {
			jo.put("id", user.getId());
			jo.put("username", user.getUsername());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		getResponse().setStatus(Status.SUCCESS_OK, "User information.");
		return new JsonRepresentation(jo);
	}
	
	@Post
	public Representation create() {
		System.out.println("The Post method of user resource was invoked.");
		UserDao userDao = (UserDao) SpringUtil.getBean("userDao");
		String username = (String) getRequest().getAttributes().get("username");
		String password = getQuery().getValues("password");

		org.oc.orchestra.dao.User user = userDao.getUser(username );
		String encryptedPassword = encryptPassword(password);
		
		String result;
		if(user == null) {
			user = new org.oc.orchestra.dao.User();
			user.setUsername(username);
			user.setPassword(encryptedPassword);
			userDao.createUser(user);
			getResponse().setStatus(Status.SUCCESS_CREATED, "User is created.");
			result = "User is created.";
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "User already exists.");
			result = "User already exists.";
		}
		return new StringRepresentation(result);
	}

	private String encryptPassword(String password) {
		PasswordService passwordService = null;
		RealmSecurityManager secManager = (RealmSecurityManager) SecurityUtils.getSecurityManager();
		for(Realm realm : secManager.getRealms()) {
			if(realm.getName().equals("userRealm")) {
				CredentialsMatcher matcher = ((AuthenticatingRealm)realm).getCredentialsMatcher();
				passwordService = ((PasswordMatcher)matcher).getPasswordService();
			}
		}
		
		String encryptedPassword = passwordService.encryptPassword(password);
		System.out.println(encryptedPassword);
		return encryptedPassword;
	}
	
	@Put
	public Representation update() {
		System.out.println("The PUT method of user resource was invoked.");
		UserDao userDao = (UserDao) SpringUtil.getBean("userDao");
		String username = (String) getRequest().getAttributes().get("username");
		String password = getQuery().getValues("password");

		org.oc.orchestra.dao.User user = userDao.getUser(username );
		
		String encryptedPassword;
		encryptedPassword = encryptPassword(password);
		
		if(user == null) {
			user = new org.oc.orchestra.dao.User();
			user.setUsername(username);
			user.setPassword(encryptedPassword);
			userDao.createUser(user);
		} else {
			user.setPassword(encryptedPassword);
			userDao.updateUser(user);
		}
		getResponse().setStatus(Status.SUCCESS_OK, "User is updated.");
		return new StringRepresentation("User " + username + " is updated.");
		
	}
	
	@Delete
	public Representation delete() {
		System.out.println("The DELETE method of user resource was invoked.");
		UserDao userDao = (UserDao) SpringUtil.getBean("userDao");
		org.oc.orchestra.dao.User user = new org.oc.orchestra.dao.User();
		String username = (String) getRequest().getAttributes().get("username");
		user.setUsername(username);
		userDao.deleteUser(username);
		getResponse().setStatus(Status.SUCCESS_OK, "User is deleted.");
		return new StringRepresentation("User " + username + " is deleted.");
		
	}
}
