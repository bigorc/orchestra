package org.oc.orchestra.rest;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oc.orchestra.dao.ClientDao;
import org.oc.util.SpringUtil;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class Clients extends ServerResource {

	@Get
	public Representation listClients() {
		ClientDao clientDao = (ClientDao) SpringUtil.getBean("clientDao");
		List<org.oc.orchestra.dao.Client> clients = clientDao.findAll();
		JSONArray json = new JSONArray();
		for(org.oc.orchestra.dao.Client client: clients) {
			JSONObject jo = new JSONObject();
			try {
				jo.put("name", client.getName());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			json.put(jo);
		}
		return new JsonRepresentation(json);
	}
}
