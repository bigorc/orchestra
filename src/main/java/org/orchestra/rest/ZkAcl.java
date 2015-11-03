package org.orchestra.rest;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.orchestra.dao.RolePermissionRelationDao;
import org.orchestra.util.SpringUtil;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ZkAcl extends ServerResource {
	@Get
	public Representation getAcl() throws JSONException {
		String clientname = (String) getRequest().getAttributes().get("clientname");
		JSONObject json = new JSONObject();
		JSONArray jarr = new JSONArray();
		
		RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
		List<String> roles = rpDao.getRolesByPermission(clientname);
		List<String> allRoles = rpDao.getRolesByPermission("*");
		roles.addAll(allRoles);
		
		for(String rolename : roles) {
			JSONObject rjson = new JSONObject();
			rjson .put("name", rolename);
			jarr.put(rjson);
		}
		json.put("roles", jarr);
		return new JsonRepresentation(json);
	}
}
