package org.orchestra.dao;

import java.util.List;

public interface RoleDao {
	public void create(Role role);
//	public void update(Role resource);
	public void delete(Role role);
	public Role read(String roleName);
	public List<Role> findAll();
	public void delete(String string);
}
