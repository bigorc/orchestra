package org.orchestra.builder;

import org.orchestra.json.Json;
import org.orchestra.resource.Resource;

public interface Builder {
	static String state_base = "org.oc.orchestra.state.";
	Resource makeResource(Json json);
}
