package org.oc.orchestra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service("clientDao")
public class ClientDaoImpl extends JdbcDaoSupport implements ClientDao {

	@Autowired
	public ClientDaoImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public void create(Client client) {
		getJdbcTemplate().update(
			      "INSERT INTO clients (NAME, CREATOR, UPDATED_AT) VALUES (?, ?, ?)",
			        new Object[] {
			        client.getName(),
			        client.getCreator(),
			        client.getUpdated_at()
			      }
			    );
	}

	@Override
	public Client getClient(String clientname) {
		Client client = null;
		try {
			client = getJdbcTemplate().
				      queryForObject("SELECT * FROM clients WHERE NAME = ?",
				      new Object[] { clientname },
				      new ClientMapper()
				      );
		} catch (EmptyResultDataAccessException e) {
			
		}
		return client;
	}

	@Override
	public void update(Client client) {
		getJdbcTemplate().update(
			      "UPDATE clients SET updated_by=?,updated_at=? WHERE name=?",
			        new Object[] {
			        client.getUpdated_by(),
			        client.getUpdated_at(),
			        client.getName()
			      }
			    );
	}

	@Override
	public void delete(String clientname) {
		getJdbcTemplate().update("DELETE FROM clients WHERE name = ?",
				new Object[] { clientname });
	}

	private class ClientMapper implements RowMapper<Client> {

		@Override
		public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
			Client client = new Client();
			client.setName(rs.getString("NAME"));
			client.setCreator(rs.getString("CREATOR"));
			client.setCreated_at(rs.getTimestamp("created_at"));
			client.setUpdated_by(rs.getString("UPDATED_BY"));
			client.setUpdated_at(rs.getTimestamp("UPDATED_AT"));
			return client;
		}

		
	}
}
