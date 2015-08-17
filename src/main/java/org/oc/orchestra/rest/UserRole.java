package org.oc.orchestra.rest;

import org.oc.orchestra.dao.UserRoleRelationDao;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class UserRole extends ServerResource {

	@Get
	public Representation userHasRole() {
		String username = (String) getRequest().getAttributes().get("username");
		String rolename = (String) getRequest().getAttributes().get("rolename");
		System.out.println("username:" + username + "    rolename:" + rolename);
		UserRoleRelationDao ur = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
		if(ur.hasUserRole(username, rolename)) {
			getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "User has the role.");
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "User hasn't the role.");
		}
		return null;
	}
}
