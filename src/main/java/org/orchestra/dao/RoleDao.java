package org.orchestra.dao;

import java.util.List;

public interface RoleDao {
	public void create(Role role);
//	public void update(Role resource);
	public void delete(String roleName);
	public Role read(String roleName);
	public List<Role> findAll();
}
