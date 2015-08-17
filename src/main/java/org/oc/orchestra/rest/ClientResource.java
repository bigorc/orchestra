package org.oc.orchestra.rest;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.oc.orchestra.dao.Resource;
import org.oc.orchestra.dao.ResourceDao;
import org.oc.util.SpringUtil;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class ClientResource extends ServerResource {
	Map<String, String> resState = new HashMap<String, String>();
	
	@Put
	public Representation createResource() {
		String clientName = (String) getRequest().getAttributes().get("clientName");
		String resourceName = (String) getRequest().getAttributes().get("resourceName");
		String resourceType = (String) getRequest().getAttributes().get("resourceType");
		String username = getQuery().getValues("u");
		String state = (String) getRequest().getAttributes().get("state");
		ResourceDao resDao = (ResourceDao) SpringUtil.getBean("resourceDao");
		Resource resource = resDao.read(clientName, resourceType, resourceName);
		if(resource == null) {
			resource = new Resource();
			resource.setClient(clientName);
			resource.setType(resourceType);
			resource.setName(resourceName);
			resource.setCreator(username);
			Timestamp t = new Timestamp(new Date().getTime());
			resource.setCreated_at(t);
			resource.setUpdated_by(username);
			resource.setUpdated_at(t);
			resource.setState(state);
			resDao.create(resource);
			return new StringRepresentation(clientName + "/" + resourceType + "/" + 
					resourceName + " created\n");
		} else {
			resource.setUpdated_by(username);
			resource.setUpdated_at(new Timestamp(new Date().getTime()));
			resource.setState(state);
			resDao.update(resource);
			return new StringRepresentation(clientName + "/" + resourceType + "/" + 
					resourceName + " updated to " + state);
		}
	}
	
	@Get
	public Representation getResourceState() {
		String clientName = (String) getRequest().getAttributes().get("clientName");
		String resourceType = (String) getRequest().getAttributes().get("resourceType");
		String resourceName = (String) getRequest().getAttributes().get("resourceName");
		ResourceDao resDao = (ResourceDao) SpringUtil.getBean("resourceDao");
		Resource resource = resDao.read(clientName, resourceType, resourceName);
		return new StringRepresentation(resource.getState());
	}
}
