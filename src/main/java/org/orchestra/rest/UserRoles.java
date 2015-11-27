package org.orchestra.rest;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.orchestra.dao.UserRole;
import org.orchestra.dao.UserRoleRelationDao;
import org.orchestra.util.SpringUtil;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRoles extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(UserRoles.class);
	@Get
	public Representation getAll() throws JSONException {
		UserRoleRelationDao urdao = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
		List<org.orchestra.dao.UserRole> urs = urdao.findAll();
		JSONObject json = new JSONObject();
		for(UserRole ur : urs) {
			if(!json.has(ur.getUsername())) {
				json.put(ur.getUsername(), new JSONArray());
			}
			JSONArray roles = (JSONArray) json.get(ur.getUsername());
			roles.put(ur.getRolename());
		}
		
		return new JsonRepresentation(json);
	}
	
}
