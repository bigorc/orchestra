package org.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.orchestra.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDao")
public class UserDaoImpl extends JdbcDaoSupport  implements UserDao {
	
	@Autowired
	public UserDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public List<User> findAll() {
		List<User> users = getJdbcTemplate().
			      query("SELECT * FROM users",
			      new UserMapper()
			      );
		return users;
	}

	@Override
	@Transactional
	public void createUser(User user) {
		getJdbcTemplate().update(
			      "INSERT INTO users (USERNAME, PASSWORD) VALUES (?, ?)",
			        new Object[] {
			        user.getUsername(),
			        user.getPassword()
			      }
			    );
	}

	@Override
	public User getUser(String userName) {
		User user = null;
		try {
			user = getJdbcTemplate().queryForObject(
					"SELECT * FROM users WHERE username = ?",
					new Object[] {
							userName
					},
					new UserMapper()
					);
		} catch (EmptyResultDataAccessException e) {

		}
		return user;
	}
	
	@Override
	public void deleteUser(String userName) {
		getJdbcTemplate().update("DELETE FROM users WHERE USERNAME = ?",
				new Object[] { userName });
	}
		
	private class UserMapper implements RowMapper<User>{
		UserRoleRelationDao urDao;
	    @Override
	    public User mapRow(ResultSet rs, int rowNum)
	    		throws SQLException {
	    	urDao = (UserRoleRelationDao) SpringUtil.getBean("userRoleRelationDao");
	    	User user = new User();
	    	user.setId(rs.getInt("ID"));
	    	user.setUsername(rs.getString("USERNAME"));
	    	user.setPassword((rs.getString("PASSWORD")));
	    	user.setRoles(urDao.findRolesByUser(user.getUsername()));
	    	return user;
	    }
	    
	  }

	@Override
	public void updateUser(User user) {
		getJdbcTemplate().update(
			      "UPDATE users SET password=? WHERE username=?",
			        new Object[] {
			        user.getPassword(),
			        user.getUsername()
			      }
			    );
	}

	
}
