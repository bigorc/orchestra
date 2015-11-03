package org.orchestra.constraint;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.orchestra.resource.Resource;
import org.orchestra.sm.StateMachine;

public class StateMachineConstraint implements Constraint {
	private static final String resource_path = "/org/oc/orchestra/sm/";
	private String type;
	private List<String> args;
	private String state;
	
	protected StateMachineConstraint() {}
	
	public StateMachineConstraint(String type, List<String> args, String state) {
		this.type = type;
		this.args = args;
		this.state = state;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	@Override
	public boolean check() {
		StateMachine sm = new StateMachine(type, args);
		return state.equals(sm.getCurrentState());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public void enforce() {
		StateMachine sm = new StateMachine(type, args);
		sm.run(state);
	}

}
