package org.oc.orchestra.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.json.Json;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.GeneralResource;
import org.oc.orchestra.resource.Resource;
import org.oc.orchestra.state.State;

public class GeneralResourceBuilder extends ResourceFactory implements Builder {

	private static final String SM_STATE = "sm_state";
	private static final String STATE_MACHINE = "stateMachine";
	private static final String SM_ARGS = "sm_args";

	@Override
	public Resource makeResource(Json json) {
		GeneralResource resource = new GeneralResource();
		String sm = (String) json.get(STATE_MACHINE);
		resource.setStateMachine(sm);
		String state = (String) json.get(SM_STATE);
		resource.setState(state);
		String arg0 = (String) json.get("sm_arg0");
		int i = 0;
		while(json.get("sm_arg" + i) != null) {
			resource.addArg((String) json.get("sm_arg" + i));
			i++;
		}
		String args = (String) json.get(SM_ARGS);
		if(args == null) {
			resource.setArgs("");
		} else {
			resource.setArgs(args);;
		}
		resource.setClient((String) json.get("client"));
		Iterator it = json.keySet().iterator();
		while(it.hasNext()) {
			String key = (String) it.next();
			if(key.equals(STATE_MACHINE) || key.equals(SM_STATE) || key.equals(SM_ARGS)) continue;
			Object value = json.get(key);
			if(value instanceof String || value instanceof Long || value instanceof Double) {
				resource.set(key, value.toString());
			} else {
				continue;
			}
		}
		return resource;
	}

}
