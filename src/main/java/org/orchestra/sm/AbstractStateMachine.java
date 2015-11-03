package org.orchestra.sm;

public abstract class AbstractStateMachine {
	public abstract int run(String state);
	public abstract String getCurrentState();
	public abstract void start();
	public abstract String uri();
}
