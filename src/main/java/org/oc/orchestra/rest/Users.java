package org.oc.orchestra.rest;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.UserDao;
import org.oc.util.SpringUtil;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class Users extends ServerResource {
	@Override
	protected Representation get() throws ResourceException {
		System.out.println("The GET method of users resource was invoked.");
		UserDao userDao = (UserDao) SpringUtil.getBean("userDao");
		List<org.oc.orchestra.dao.User> users = userDao.findAll();
		JSONArray json = new JSONArray();
		for(org.oc.orchestra.dao.User user : users) {
			JSONObject jo = new JSONObject();
			try {
				jo.put("id", user.getId());
				jo.put("username", user.getUsername());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			json.put(jo);
		}
		return new JsonRepresentation(json);
	}

}
