package org.oc.orchestra.state;

import org.oc.orchestra.resource.GeneralResource;
import org.oc.orchestra.resource.Resource;
import org.oc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ServiceState implements State {
	RUNNING {

		@Override
		public void apply(Resource resource) throws StateException {
			start((String) ((GeneralResource)resource).get("name"));
		}
		
	},
	STOPPED {

		@Override
		public void apply(Resource resource) throws StateException {
			stop((String) ((GeneralResource)resource).get("name"));
		}
		
	};
	
	private static final Logger LOG = LoggerFactory.getLogger(ServiceState.class);
	
	public ServiceState start(String name) throws StateException {
		Command cmd = new LocalCommand("sudo service " + name + " start");
		String output = cmd.execute();
		if(isRunning(name)) {
			return RUNNING;
		} else {
			LOG.error("Failed to start service " + name);
			throw new StateException(output);
		}
	}
	
	public ServiceState stop(String name) throws StateException {
		Command cmd = new LocalCommand("sudo service " + name + " stop");
		String output = cmd.execute();
		if(!isRunning(name)) {
			return STOPPED;
		} else {
			LOG.error("Failed to stop service " + name);
			throw new StateException(output);
		}
	}
	
	public ServiceState restart(String name) throws StateException {
		Command cmd = new LocalCommand("sudo service " + name + " restart");
		String output = cmd.execute();
		if(isRunning(name)) {
			return RUNNING;
		} else {
			LOG.error("Failed to restart service " + name);
			throw new StateException(output);
		}
	}
	
	public boolean isRunning(String name) {
		Command cmd = new LocalCommand("service --status-all");
		String output = cmd.execute();
		if(output.contains("[ - ]  " + name)) {
			return false;
		} else {
			return true;
		}
	}
}
