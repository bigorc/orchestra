package org.oc.orchestra.dao;

public interface ResourceDao {
	public void create(Resource resource);
	public void update(Resource resource);
	public void delete(String clientName, String resourceName);
	public Resource read(String clientName, String resourceType, String resourceName);
}
