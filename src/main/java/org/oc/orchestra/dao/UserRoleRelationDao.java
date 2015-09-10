package org.oc.orchestra.dao;

import java.util.List;

public interface UserRoleRelationDao {
	public boolean hasUserRole(String username, String rolename);
	public void addUserRole(String username, String rolename);
	public void removeUserRole(String username, String rolename);
	
}
