package org.orchestra.dao;

import java.util.List;

public interface ClientDao {
	public void create(Client client);
	public Client getClient(String clientname);
	public void update(Client client);
	public void delete(String clientname);
	public List<Client> findAll();
}
