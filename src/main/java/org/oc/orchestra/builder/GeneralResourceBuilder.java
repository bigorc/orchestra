package org.oc.orchestra.builder;

import java.util.Iterator;

import org.oc.json.Json;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.GeneralResource;
import org.oc.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralResourceBuilder extends ResourceFactory implements Builder {
	private final Logger logger = LoggerFactory.getLogger(GeneralResourceBuilder.class);
	private static final String SM_STATE = "sm_state";
	private static final String STATE_MACHINE = "stateMachine";
	private static final String SM_ARGS = "sm_args";

	@Override
	public Resource makeResource(Json json) {
		GeneralResource resource = new GeneralResource();
		
		String sm = (String) json.get(STATE_MACHINE);
		logger.debug(STATE_MACHINE + ":" + json.get(STATE_MACHINE));
		resource.setStateMachine(sm);
		
		String state = (String) json.get(SM_STATE);
		logger.debug(SM_STATE + ":" + json.get(SM_STATE));
		resource.setState(state);
		
		int i = 0;
		while(json.get("sm_arg" + i) != null) {
			logger.debug("sm_arg" + i + ":" + json.get("sm_arg" + i));
			resource.addArg((String) json.get("sm_arg" + i));
			i++;
		}
		
		String args = (String) json.get(SM_ARGS);
		logger.debug(SM_ARGS + ":" + json.get(SM_ARGS));
		if(args == null) {
			resource.setArgs("");
		} else {
			resource.setArgs(args);;
		}
		
		logger.debug("block:" + json.get("block"));
		if("true".equalsIgnoreCase((String) json.get("block"))) resource.setBlock(true);
		
		logger.debug("client:" + json.get("client"));
		resource.setClient((String) json.get("client"));
		
		Iterator<?> it = json.keySet().iterator();
		while(it.hasNext()) {
			String key = (String) it.next();
			if(key.equals(STATE_MACHINE) || key.equals(SM_STATE) || key.equals(SM_ARGS)) continue;
			Object value = json.get(key);
			if(value instanceof String || value instanceof Long || value instanceof Double) {
				resource.set(key, value.toString());
				logger.debug(key + ":" + value);
			} else {
				continue;
			}
		}
		return resource;
	}

}
