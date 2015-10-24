package org.oc.orchestra.resource;

import org.json.simple.JSONObject;
import org.oc.json.Json;
import org.oc.orchestra.client.Client;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.sm.AbstractStateMachine;

public abstract class Resource extends AbstractStateMachine {
	protected String default_state;
	protected String client;
	protected boolean block;
	private String uri;
	protected Coordinator coordinator;
	protected Json json;

	public Json getJson() {
		return json;
	}

	public void setJson(Json json) {
		this.json = json;
	}

	public boolean isBlock() {
		return block;
	}

	public void setBlock(boolean block) {
		this.block = block;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}
	
	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}
	public String uri() {
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
