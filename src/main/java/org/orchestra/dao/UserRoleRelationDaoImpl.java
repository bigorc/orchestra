package org.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;

@Service("userRoleRelationDao")
public class UserRoleRelationDaoImpl extends JdbcDaoSupport  implements UserRoleRelationDao {

	@Autowired
	public UserRoleRelationDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
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

	@Override
	public void addUserRole(String username, String rolename) {
		getJdbcTemplate().update(
			      "INSERT INTO user_roles (USERNAME, ROLE_NAME) VALUES (?, ?)",
			        new Object[] {
			        username,
			        rolename
			      }
			    );
	}

	@Override
	public void removeUserRole(String username, String rolename) {
		getJdbcTemplate().update("DELETE FROM user_roles WHERE USERNAME = ? AND role_name = ?",
				new Object[] { username, rolename });
	}

}
