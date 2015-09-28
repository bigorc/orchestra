package org.oc.orchestra.builder;

import java.util.List;

import org.json.simple.JSONArray;
import org.oc.json.Json;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.Container;
import org.oc.orchestra.resource.Resource;

public class ContainerBuilder implements Builder {

	@Override
	public Resource makeResource(Json json) {
		Container container = new Container();
		if("true".equalsIgnoreCase((String) json.get("block"))) container.setBlock(true);
		container.setClient((String) json.get("client"));
		List<Resource> resources = ResourceFactory.makeResources((JSONArray) json.get("resources"));
		container.setResources(resources);
		return container;
	}

}
