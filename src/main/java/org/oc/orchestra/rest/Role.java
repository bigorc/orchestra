package org.oc.orchestra.rest;

import org.apache.shiro.codec.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.RoleDao;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Role extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Role.class);
	
	@Get
	public Representation getRole() throws JSONException {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Role:" + rolename );
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		org.oc.orchestra.dao.Role role = roleDao.read(rolename);
		if(role == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Client not found.");
			return null;
		}
		JSONObject json = new org.json.JSONObject();
		json.put("name", role.getName());
		getResponse().setStatus(Status.SUCCESS_OK, "Role exists.");
		return new JsonRepresentation(json);
	}
	
	@Post
	public Representation createRole() throws JSONException {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Creating Role:" + rolename );
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		org.oc.orchestra.dao.Role role = roleDao.read(rolename);
		if(role == null) {
			role = new org.oc.orchestra.dao.Role();
			role.setName(rolename);
			roleDao.create(role);
			JSONObject json = new org.json.JSONObject();
			json.put("name", rolename);
			getResponse().setStatus(Status.SUCCESS_CREATED, "role is created.");
			return new JsonRepresentation(json );
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Role already exists.");
			return new StringRepresentation("Role already exists.");
		}
	}
	
	@Delete
	public Representation deleteRole() {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Creating Role:" + rolename );
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		org.oc.orchestra.dao.Role role = roleDao.read(rolename);
		if(role == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Role not found.");
			return null;
		}
		roleDao.delete(rolename);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "Role is deleted.");
		return null;
	}
}
