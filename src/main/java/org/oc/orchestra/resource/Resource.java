package org.oc.orchestra.resource;

import org.json.simple.JSONObject;
import org.oc.orchestra.client.Client;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.sm.AbstractStateMachine;

public abstract class Resource extends AbstractStateMachine {
	protected String default_state;
	protected String client;
	private String uri;
	protected Coordinator coordinator;

	public Coordinator getCoordinator() {
		return coordinator;
	}
	
	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}
	public String getUri() {
		return uri;
	}

	public String getClient() {
		return client;
	}
	
	public void setClient(String client) {
		this.client = client;
	}
	
	public String getDefault_state() {
		return default_state;
	}
	
	public void setDefault_state(String default_state) {
		this.default_state = default_state;
	}
	
	public String getState() {
		return default_state;
	}
	public abstract void realize();
	public abstract String toRO();
	public abstract JSONObject toJson();
}
