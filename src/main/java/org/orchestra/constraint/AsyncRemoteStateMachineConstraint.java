package org.orchestra.constraint;

import java.util.List;

import org.orchestra.coordinate.Coordinator;
import org.orchestra.coordinate.Curator;

public class AsyncRemoteStateMachineConstraint extends DistributedSMConstraint 
		implements SMable{
	private String sm;
	private List<String> args;
	private String state;
	
	public AsyncRemoteStateMachineConstraint(String sm, 
			List<String> args, String state) {
		this.sm = sm;
		this.args = args;
		this.state = state;
	}

	public AsyncRemoteStateMachineConstraint() {}

	@Override
	public void enforce() {
		coordinator.asyncAssignSMTask(sm, args, state);
	}

	@Override
	public boolean check() {
		return state.equals(coordinator.getSMState(sm, args));
	}

	@Override
	public String getSM() {
		return sm;
	}

	@Override
	public void setSM(String sm) {
		this.sm = sm;
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
	public String getState() {
		return state;
	}

	@Override
	public void setState(String state) {
		this.state = state;
	}

}
