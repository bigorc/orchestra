package org.oc.orchestra.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.oc.orchestra.client.Client;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.coordinate.Curator;
import org.oc.orchestra.sm.StateMachine;
import org.oc.orchestra.state.StateException;

public class GeneralResource extends Resource {
	private static final int sleep_interval = 1000;
	Map<String, String> properties = new HashMap<String, String>();
	String stateMachine;
	String args;
	String state;
	private ArrayList<String> argList;
	
	public String getStateMachine() {
		return stateMachine;
	}
	public void setStateMachine(String sm) {
		this.stateMachine = sm;
	}
		
	public String getArgs() {
		return args;
	}
	
	public void setArgs(String args) {		
		this.args = args;
	}

	public String getState() {
		//only useful for "cmd"
		if(state == null) return "successful";
		return state;
	}
	
	@Override
	public void realize() {
		if(client == null || client.equals(Client.getName())) {
			run(state);
			if(block) {
				long timeout = 0;
				long timeSlept = 0;
				if(timeout  > 0) {
					while((timeSlept < timeout) && !getCurrentState().equals(getState())) {
						try {
							Thread.sleep(sleep_interval);
							timeSlept += sleep_interval;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {
					while(!getCurrentState().equals(getState())) {
						try {
							Thread.sleep(sleep_interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			if(block) {
				Client.getCoordinator(client).assignResourceTask(this);
			} else {
				Client.getCoordinator(client).asyncAssignResourceTask(this);
			}
		}
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public void set(String key, String object) {
		properties.put(key, object);
	}

	public Object get(String key) {
		return properties.get(key);
	}
	
	@Override
	public int run(String stateName) {
		StateMachine sm = new StateMachine(getStateMachine(), getArgsAsList(), properties);
		return sm.run(stateName);
	}
	
	public List<String> getArgsAsList() {
		if(argList != null) return argList;
		List<String> ol = new ArrayList<String>(Arrays.asList(args.split(" ")));
		return ol;
//		List<String> aList = new ArrayList<String>(); 
//		if(args == null) {
//			return aList;
//		} else {
//			for(int i=0;i < ol.size();i++) {
//				//two kinds of double quoted arguments
//				//1. echo -e "hello world", etc.
//				//2. somecommand -name="John Smith" etc.
//				if(!ol.get(i).startsWith("\"") && !ol.get(i).contains("=\\\"")) {
//					if(i < aList.size()) {
//						aList.set(i, ol.get(i));
//					} else {
//						aList.add(ol.get(i));
//					}
//				} else {
//					StringBuffer str = new StringBuffer(ol.get(i));
//					for(int j=i + 1;j < ol.size();j++) {
//						if(ol.get(j).endsWith("\"")) {
//							for(int k=i + 1;k <= j;k++) {
//								str.append(' ');
//								str.append(ol.get(k));
//							}
//							for(int k=i + 1;k <= j;k++) {
//								ol.remove(i + 1);
//							}
//							String unquoted;
//							if(str.toString().startsWith("\"")) {
//								unquoted = str.toString().substring(1, str.length() - 1);
//							} else {
//								unquoted = str.toString();
//							}
//							if(i < aList.size()) {
//								aList.set(i, unquoted);
//							} else {
//								aList.add(unquoted);
//							}
//							break;
//						} else {
//							if(i < aList.size()) {
//								aList.set(i, ol.get(i));
//							} else {
//								aList.add(ol.get(i));
//							}
//						}
//					}
//				}
//				
//			}
//		}
//		return aList;
	}
	
	@Override
	public String getCurrentState() {
		if(client == null || client.equals(Client.getName())) {
			StateMachine sm = new StateMachine(getStateMachine(), getArgsAsList(), properties);
			String state = sm.getCurrentState();
			if(state == null) return "";
			return state;
		} else {
			return Client.getCoordinator(client).getState(uri(), toRO());
		}
	}
	
	@Override
	public void start() {
		StateMachine sm = new StateMachine(getStateMachine(), getArgsAsList(), properties);
		sm.start();
	}
	
	@Override
	public String toRO() {
		return toJson().toJSONString();
	}
	
	@Override
	public String uri() {
		StateMachine sm = new StateMachine(getStateMachine(), getArgsAsList(), properties);
		StringBuffer uri = new StringBuffer();
		uri.append("gr_");
		uri.append(sm.uri());
		
		return uri.toString();
	}
	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("stateMachine", stateMachine);
		if(args != null) json.put("sm_args", args);
		if(state != null) json.put("sm_state", state);
		for(Entry<String, String> p : properties.entrySet()) {
			json.put(p.getKey(), p.getValue());
		}
		return json;
	}
	public void addArg(String arg) {
		if(argList == null) {
			argList = new ArrayList<String>();
		}
		argList.add(arg);
	}
}
