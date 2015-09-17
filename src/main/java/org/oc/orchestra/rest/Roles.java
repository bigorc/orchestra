package org.oc.orchestra.rest;

import java.util.List;

import org.apache.shiro.codec.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.Role;
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
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Roles extends ServerResource {
	private final Logger logger = LoggerFactory.getLogger(Roles.class);
	
	@Get
	public Representation getAllRoles() throws JSONException {
		RoleDao roleDao = (RoleDao) SpringUtil.getBean("roleDao");
		List<org.oc.orchestra.dao.Role> roles = roleDao.findAll();
		RolePermissionRelationDao rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
		
		
		JSONArray json = new JSONArray();
		for(org.oc.orchestra.dao.Role role : roles) {
			String rpStr = "";
			
			String rolename = role.getName();
			List<RolePermission> rps = rpDao.getRolePermission(rolename);
			for(RolePermission rp : rps) {
				rpStr += rp.getPermission() + ";";
			}
			if(rpStr.length() > 0) rpStr = rpStr.substring(0, rpStr.length() - 1);
			
			JSONObject jo = new JSONObject();
			try {
				jo.put("name", role.getName());
				jo.put("permission", rpStr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			json.put(jo);
		}
		return new JsonRepresentation(json);
	}
	
}
