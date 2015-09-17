package org.oc.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;

@Service("rolePermissionDao")
public class RolePermissionRelationDaoImpl extends JdbcDaoSupport implements RolePermissionRelationDao {
	
	@Autowired
	public RolePermissionRelationDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public List<RolePermission> getRolePermission(String rolename) {
		List<RolePermission> list = getJdbcTemplate().
	      query("SELECT * FROM roles_permissions WHERE role_name=?",
	    		  new Object[] { rolename },
	    		  new RolePermissionMapper()
	      );
		return list;
	}
	
	private class RolePermissionMapper implements RowMapper<RolePermission>{

		@Override
		public RolePermission mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			RolePermission rp = new RolePermission();
			rp.setRoleName(rs.getString("ROLE_NAME"));
			rp.setPermission(rs.getString("PERMISSION"));
			return rp;
		}
		
	}
	
	@Override
	public void addRolePermission(String rolename, String permission) {
		getJdbcTemplate().update(
			      "INSERT INTO roles_permissions (ROLE_NAME, PERMISSION) VALUES (?, ?)",
			        new Object[] {
			        rolename,
			        permission
			      }
			    );
	}

	@Override
	public void updateRolePermission(String rolename, String oldperm, String newperm) {
		getJdbcTemplate().update(
			      "UPDATE roles_permissions SET PERMISSION = ? WHERE ROLE_NAME = ? AND PERMISSION = ?",
			        new Object[] {
			        newperm,
			        rolename,
			        oldperm
			      }
			    );
	}

	@Override
	public void removeRolePermission(String rolename, String permission) {
		if(permission == null) {
			getJdbcTemplate().update("DELETE FROM roles_permissions WHERE role_name = ?",
					new Object[] { rolename});
		} else {
			getJdbcTemplate().update("DELETE FROM roles_permissions WHERE role_name = ? AND PERMISSION = ?",
					new Object[] { rolename, permission });
		}
	}

}
