package org.oc.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;


public class UserRoleRelationDaoImpl extends JdbcDaoSupport  implements UserRoleRelationDao {


	@Override
	public boolean hasUserRole(String username, String rolename) {
		List<UserRole> userRoles = getJdbcTemplate().
			      query("SELECT * FROM user_roles WHERE username=? AND role_name=?",
			    		  new Object[] { username, rolename },
			    		  new UserRoleMapper()
			      );
		if(userRoles.size() == 0) return false;
		return true;
	}
	
	private class UserRoleMapper implements RowMapper<UserRole>{

	    @Override
	    public UserRole mapRow(ResultSet rs, int rowNum)
	        throws SQLException {
	      UserRole userRole = new UserRole();
	      userRole.setUsername(rs.getString("USERNAME"));
	      userRole.setRolename(rs.getString("ROLE_NAME"));
	      return userRole;
	    }
	    
	  }

}
