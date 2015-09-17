package org.oc.orchestra.dao;

import java.util.List;

public interface RolePermissionRelationDao {
	public List<RolePermission> getRolePermission(String rolename);
	public void addRolePermission(String rolename, String permission);
	public void updateRolePermission(String rolename, String oldperm, String newperm);
	public void removeRolePermission(String rolename, String permission);
	
}
