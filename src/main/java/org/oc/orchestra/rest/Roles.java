package org.oc.orchestra.rest;

import java.util.List;

import org.apache.shiro.codec.Base64;
import org.json.JSONArray;
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

public class Roles extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Roles.class);
	
	@Get
	public Representation getAllRoles() throws JSONException {
		String rolename = (String) getRequest().getAttributes().get("rolename");
		logger.info("Role:" + rolename );
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		List<org.oc.orchestra.dao.Role> roles = roleDao.findAll();
		JSONArray json = new JSONArray();
		for(org.oc.orchestra.dao.Role role : roles) {
			JSONObject jo = new JSONObject();
			try {
				jo.put("name", role.getName());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			json.put(jo);
		}
		return new JsonRepresentation(json);
	}
	
}
