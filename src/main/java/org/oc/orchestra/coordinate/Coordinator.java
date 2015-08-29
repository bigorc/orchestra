package org.oc.orchestra.coordinate;

import java.util.List;
import java.util.concurrent.Future;

import org.oc.orchestra.resource.Resource;

public interface Coordinator {

	String getSMState(String type, List<String> args);
	
	String getProState(String pro);
	
	Future<String> asyncGetSMState(String type, List<String> args);
	
	Future<String> asyncGetProState(String pro);
	
	boolean assignSMTask(String type, List<String> args, String state);
	
	boolean assignProTask(String pro);
	
	String asyncAssignSMTask(String type, List<String> args, String state);
	
	String asyncAssignProTask(String pro);
	
	public abstract void setClient(String client);
	
	public abstract String getClient();

	void assignResourceTask(Resource resource);

	Future<String> asyncGeState(String uri, String config);

	String getState(String uri, String config);

	String asyncAssignTask(String task, String config);

	void asyncAssignResourceTask(Resource resource);
}
