package org.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.orchestra.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
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
	
	@Override
	public List<Role> findRolesByUser(String username) {
		List<Role> userRoles = getJdbcTemplate().
			      query("SELECT DISTINCT * FROM user_roles WHERE username=? ",
			    		  new Object[] { username },
			    		  new UserRoleResultSetExtractor()
			      );
		return userRoles;
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

	@Override
	public List<UserRole> findAll() {
		List<UserRole> userRoles = getJdbcTemplate().
			      query("SELECT * FROM user_roles ",
			    		  new UserRoleMapper()
			      );
		return userRoles;
	}
	
	private class UserRoleResultSetExtractor implements ResultSetExtractor<List<Role>> {
		RolePermissionRelationDao rpDao;
		@Override
		public List<Role> extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			rpDao = (RolePermissionRelationDao) SpringUtil.getBean("rolePermissionDao");
			List<Role> roles = new ArrayList<Role>();
			while(rs.next()){
				Role role = new Role();
				String rolename = rs.getString("ROLE_NAME");
				role.setName(rolename);
				role.setPermissions(rpDao.getPermissionsByRole(rolename));
				roles.add(role);
			}
			return roles;
		}

	}


}
