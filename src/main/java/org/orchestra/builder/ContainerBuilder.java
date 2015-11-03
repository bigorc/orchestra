package org.orchestra.builder;

import java.util.List;

import org.json.simple.JSONArray;
import org.orchestra.ResourceFactory;
import org.orchestra.json.Json;
import org.orchestra.resource.Container;
import org.orchestra.resource.Resource;

public class ContainerBuilder implements Builder {

	@Override
	public Resource makeResource(Json json) {
		Container container = new Container();
		if("true".equalsIgnoreCase((String) json.get("block"))) container.setBlock(true);
		container.setClient((String) json.get("client"));
		List<Resource> resources = ResourceFactory.makeResources((JSONArray) json.get("resources"), json);
		container.setResources(resources);
		return container;
	}

}
