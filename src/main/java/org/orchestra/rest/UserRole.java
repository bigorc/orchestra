package org.orchestra.rest;

import org.orchestra.dao.UserRoleRelationDao;
import org.orchestra.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRole extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(UserRole.class);
	@Get
	public Representation userHasRole() {
		String username = (String) getRequest().getAttributes().get("username");
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.debug("username:" + username + "    rolename:" + rolename);
		UserRoleRelationDao ur = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
		if(ur.hasUserRole(username, rolename)) {
			getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "User has the role.");
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "User hasn't the role.");
		}
		return null;
	}
	
	@Post
	public Representation addUserRole() {
		String username = (String) getRequest().getAttributes().get("username");
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.debug("username:" + username + "    rolename:" + rolename);
		UserRoleRelationDao ur = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
		if(ur.hasUserRole(username, rolename)) {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "User already has the role");
		} else {
			ur.addUserRole(username, rolename);
			getResponse().setStatus(Status.SUCCESS_CREATED, "Add role to the user");
		}
		return null;
	}
	
	@Delete
	public Representation removeUserRole() {
		String username = (String) getRequest().getAttributes().get("username");
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.debug("username:" + username + "    rolename:" + rolename);
		UserRoleRelationDao ur = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
		if(ur.hasUserRole(username, rolename)) {
			ur.removeUserRole(username, rolename);
			getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "User removed the role");
		}
		return null;
	}
}
