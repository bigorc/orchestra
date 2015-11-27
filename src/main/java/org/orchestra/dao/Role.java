package org.orchestra.dao;

import java.util.ArrayList;
import java.util.List;

public class Role {
	private String name;
	private List<String> permissions = new ArrayList<String>();
	
	public List<String> getPermissions() {
		return permissions;
	}

	public void addPermissions(String permission) {
		this.permissions.add(permission);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}
}
