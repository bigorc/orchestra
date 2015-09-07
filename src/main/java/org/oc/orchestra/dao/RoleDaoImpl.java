package org.oc.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;

@Service("roleDao")
public class RoleDaoImpl extends JdbcDaoSupport implements RoleDao {

	@Autowired
	public RoleDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public void create(Role role) {
		getJdbcTemplate().update(
			      "INSERT INTO roles (name) VALUES (?)",
			        new Object[] {
			        role.getName()
			      }
			    );
	}

	@Override
	public Role read(String roleName) {
		Role role = null;
		try {
			role = getJdbcTemplate().
				      queryForObject("SELECT * FROM roles WHERE name = ?",
				      new Object[] { roleName },
				      new RoleMapper()
				      );
		} catch (EmptyResultDataAccessException e) {
			
		}
		return role;
	}
//
//	@Override
//	public void update(Role role) {
//		getJdbcTemplate().update(
//			      "UPDATE roles SET name=?",
//			        new Object[] {
//			        role.getName()
//			      }
//			    );
//	}

	@Override
	public void delete(String roleName) {
		getJdbcTemplate().update("DELETE FROM roles WHERE name = ?",
				new Object[] { roleName });
	}
	private class RoleMapper implements RowMapper<Role> {

		@Override
		public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
			Role role = new Role();
			role.setName(rs.getString("NAME"));
			return role;
		}

		
	}
	@Override
	public List<Role> findAll() {
		List<Role> roles = getJdbcTemplate().
			      query("SELECT * FROM roles",
			      new RoleMapper()
			      );
		return roles;
	}
}
