package org.orchestra.builder;

import org.orchestra.json.Json;
import org.orchestra.resource.Resource;

public interface Builder {
	Resource makeResource(Json json);
}
