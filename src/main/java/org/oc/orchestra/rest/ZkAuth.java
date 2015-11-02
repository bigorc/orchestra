package org.oc.orchestra.rest;

import org.oc.orchestra.dao.UserRoleRelationDao;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkAuth extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(ZkAuth.class);
	
	@Get
	public Representation get() {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		if(rolename != null) {
			String username = AuthFilter.getUserPass(getRequest())[0];
			logger.debug("User:" + username + "role:" + rolename);
			UserRoleRelationDao ur = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
			if(!ur.hasUserRole(username, rolename)) {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "User hasn't the role.");
				return new StringRepresentation("Unauthenticated.");
			}
		}
		logger.debug("Authenticated zookeeper user");
		return new StringRepresentation("Authenticated");
	}
	
}
