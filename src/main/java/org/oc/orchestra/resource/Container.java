package org.oc.orchestra.resource;

import java.util.List;
import java.util.Map.Entry;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.orchestra.client.Client;

public class Container extends Resource {
	private String state = "all_ready";
	private List<Resource> resources;
	
	@Override
	public int run(String stateName) {
		int exit = 0;
		for(Resource r: resources) {
			if(r.run(stateName) != 0) exit = -1;
		}
		return exit;
	}

	@Override
	public String getCurrentState() {
		if(client == null || client.equals(Client.getName())) {
			for(Resource r : resources) {
				if(!r.getCurrentState().equals(r.getState())) return "not_all_ready";
			}
			return "all_ready";
		} else {
			return coordinator.getState(getUri(), toRO());
		}
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public void realize() {
		if(client == null || client.equals(Client.getName())) {
			for(Resource r : resources) {
				r.realize();
			}
		} else {
			coordinator.assignResourceTask(this);
		}
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

	@Override
	public void start() {
		for(Resource r : resources) {
			r.start();
		}
	}

	@Override
	public String toRO() {
		return toJson().toJSONString();
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "container");
		JSONArray rsArr = new JSONArray();
		json.put("resources", rsArr);
		for(Resource r : resources) {
			rsArr.add(r.toJson());
		}
		return json;
	}

	@Override
	public String uri() {
		StringBuffer source = new StringBuffer();
		for(Resource r : resources) {
			source.append(r.getUri());
		}
		String md5 = new Md5Hash(source.toString()).toString();
		return "container_" + md5;
	}
}
