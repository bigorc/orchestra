package org.oc.orchestra.rest;

import java.util.List;

import org.apache.shiro.codec.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.RoleDao;
import org.oc.orchestra.dao.RolePermission;
import org.oc.orchestra.dao.RolePermissionRelationDao;
import org.oc.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
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
		RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
		List<RolePermission> rps = rpDao.getRolePermission(rolename);
		String rpStr = "";
		for(RolePermission rp : rps) {
			rpStr += rp.getPermission() + ";";
		}
		if(rpStr.length() > 0) rpStr = rpStr.substring(0, rpStr.length() - 1);
		JSONObject json = new org.json.JSONObject();
		json.put("name", role.getName());
		json.put("permissions", rpStr);
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
			//Add role
			role = new org.oc.orchestra.dao.Role();
			role.setName(rolename);
			roleDao.create(role);
			
			//Add role's permissions if there are any
			RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
			String[] permissions = getQuery().getValuesArray("permission");
			if(permissions.length > 0) {
				for(String p : permissions) {
					rpDao.addRolePermission(rolename, p);
				}
			}
			List<RolePermission> rps = rpDao.getRolePermission(rolename);
			String rpStr = "";
			for(RolePermission rp : rps) {
				rpStr += rp.getPermission() + ";";
			}
			if(rpStr.length() > 0) rpStr = rpStr.substring(0, rpStr.length() - 1);
			JSONObject json = new org.json.JSONObject();
			json.put("name", rolename);
			json.put("permissions", rpStr);
			getResponse().setStatus(Status.SUCCESS_CREATED, "role is created.");
			return new JsonRepresentation(json );
		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Role already exists.");
			return new StringRepresentation("Role already exists.");
		}
	}

	@Put
	public Representation updateRole() throws JSONException {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Updating Role:" + rolename );
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		org.oc.orchestra.dao.Role role = roleDao.read(rolename);
		if(role == null) {
			//Add role
			role = new org.oc.orchestra.dao.Role();
			role.setName(rolename);
			roleDao.create(role);
			
			//Add role's permissions if there are any
			RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
			String[] permissions = getQuery().getValuesArray("permission");
			if(permissions.length > 0) {
				for(String p : permissions) {
					rpDao.addRolePermission(rolename, p);
				}
			}
			List<RolePermission> rps = rpDao.getRolePermission(rolename);
			String rpStr = "";
			for(RolePermission rp : rps) {
				rpStr += rp.getPermission() + ";";
			}
			if(rpStr.length() > 0) rpStr = rpStr.substring(0, rpStr.length() - 1);
			JSONObject json = new org.json.JSONObject();
			json.put("name", rolename);
			json.put("permissions", rpStr);
			getResponse().setStatus(Status.SUCCESS_CREATED, "role is created.");
			return new JsonRepresentation(json );
		} else {
			//Add role's permissions if there are any
			RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
			rpDao.removeRolePermission(rolename, null);
			String[] permissions = getQuery().getValuesArray("permission");
			if(permissions.length > 0) {
				for(String p : permissions) {
					rpDao.addRolePermission(rolename, p);
				}
			}
			getResponse().setStatus(Status.SUCCESS_OK, "role is updated.");
			return new StringRepresentation("role updated");
		}
	}
	
	@Delete
	public Representation deleteRole() {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Creating Role:" + rolename );
		RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
		rpDao.removeRolePermission(rolename, null);
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
