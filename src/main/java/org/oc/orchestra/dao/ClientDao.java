package org.oc.orchestra.dao;

public interface ClientDao {
	public void create(Client client);
	public Client getClient(String clientname);
	public void update(Client client);
	public void delete(String clientname);
}
