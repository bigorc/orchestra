package org.oc.orchestra.constraint;

public class BeforeConstraint implements Constraint {
	private Constraint before;
	private Constraint after;
	
	public BeforeConstraint(Constraint before, Constraint after) {
		this.before = before;
		this.after = after;
	}
	
	@Override
	public void enforce() {
		before.enforce();
		after.enforce();
	}

	@Override
	public boolean check() {
		return before.check() && after.check();
	}

	public Constraint getBefore() {
		return before;
	}

	public void setBefore(Constraint before) {
		this.before = before;
	}

	public Constraint getAfter() {
		return after;
	}

	public void setAfter(Constraint after) {
		this.after = after;
	}

}
