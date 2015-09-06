package org.oc.orchestra.dao;

public interface RoleDao {
	public void create(Role resource);
//	public void update(Role resource);
	public void delete(String roleName);
	public Role read(String roleName);
}
