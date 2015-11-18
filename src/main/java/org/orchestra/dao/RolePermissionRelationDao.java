package org.orchestra.dao;

import java.util.List;

public interface RolePermissionRelationDao {
	public List<String> getPermissionsByRole(String rolename);
	public List<String> getRolesByPermission(String permission);
	public void addRolePermission(String rolename, String permission);
	public void updateRolePermission(String rolename, String oldperm, String newperm);
	public void removeRolePermission(String rolename, String permission);
}
