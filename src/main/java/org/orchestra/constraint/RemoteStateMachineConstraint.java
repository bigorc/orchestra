package org.orchestra.constraint;

import java.util.List;

public class RemoteStateMachineConstraint extends DistributedSMConstraint 
		implements SMable {
	private List<String> args;
	private String sm;
	private String state;

	public RemoteStateMachineConstraint(String sm, 
			List<String> args, String state) {
		this.sm= sm;
		this.args = args;
		this.state = state;
	}

	public RemoteStateMachineConstraint() {
	}

	@Override
	public void enforce() {
		coordinator.assignSMTask(sm, args, state);
	}

	@Override
	public boolean check() {
		return state.equals(coordinator.getSMState(sm, args));
	}
	
	@Override
	public List<String> getArgs() {
		return args;
	}

	@Override
	public void setArgs(List<String> args) {
		this.args = args;
	}

	@Override
	public String getSM() {
		return sm;
	}

	@Override
	public void setSM(String resource) {
		this.sm = resource;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public void setState(String state) {
		this.state = state;
	}
}
