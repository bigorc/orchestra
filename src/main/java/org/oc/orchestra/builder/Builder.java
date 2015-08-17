package org.oc.orchestra.builder;

import org.oc.json.Json;
import org.oc.orchestra.resource.Resource;

public interface Builder {
	static String state_base = "org.oc.orchestra.state.";
	Resource makeResource(Json json);
}
