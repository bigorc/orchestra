package org.oc.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;

@Service("resourceDao")
public class ResourceDaoImpl extends JdbcDaoSupport implements ResourceDao {

	@Autowired
	public ResourceDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public void create(Resource resource) {
		getJdbcTemplate().update(
			      "INSERT INTO resources (client, type, name, creator, created_at,"
			      + " updated_by, updated_at, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			        new Object[] {
			        resource.getClient(),
			        resource.getType(),
			        resource.getName(),
			        resource.getCreator(),
			        resource.getCreated_at(),
			        resource.getUpdated_by(),
			        resource.getUpdated_at(),
			        resource.getState()
			      }
			    );
	}

	@Override
	public Resource read(String clientName, String resourceType, String resourceName) {
		Resource resource = null;
		try {
			resource = getJdbcTemplate().
				      queryForObject("SELECT * FROM resources WHERE client = ? and type = ? and name = ?",
				      new Object[] { clientName, resourceType, resourceName },
				      new ResourceMapper()
				      );
		} catch (EmptyResultDataAccessException e) {
			
		}
		return resource;
	}

	@Override
	public void update(Resource resource) {
		getJdbcTemplate().update(
			      "UPDATE resources SET state=?, updated_by=?, updated_at=? WHERE client=? and type=? and name=?",
			        new Object[] {
			        resource.getState(),
			        resource.getUpdated_by(),
			        resource.getUpdated_at(),
			        resource.getClient(),
			        resource.getType(),
			        resource.getName()
			      }
			    );
	}

	@Override
	public void delete(String clientName, String resourceName) {
		// TODO Auto-generated method stub

	}
	private class ResourceMapper implements RowMapper<Resource> {

		@Override
		public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
			Resource resource = new Resource();
			resource.setId(rs.getInt("ID"));
			resource.setType(rs.getString("TYPE"));
			resource.setName(rs.getString("NAME"));
			resource.setState(rs.getString("STATE"));
			resource.setClient(rs.getString("CLIENT"));
			resource.setCreator(rs.getString("CREATOR"));
			resource.setCreated_at(rs.getTimestamp("CREATED_AT"));
			resource.setUpdated_by(rs.getString("UPDATED_BY"));
			resource.setUpdated_at(rs.getTimestamp("UPDATED_AT"));
			return resource;
		}

		
	}
}
