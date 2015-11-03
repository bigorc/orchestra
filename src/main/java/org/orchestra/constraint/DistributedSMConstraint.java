package org.orchestra.constraint;

import org.orchestra.coordinate.Coordinator;

public abstract class DistributedSMConstraint implements Constraint {
	protected Coordinator coordinator;
	public void setClient(String client) {
		coordinator.setClient(client);
	}
	public String getClient() {
		return coordinator.getClient();
	}
	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}
	public Coordinator getCoordinator() {
		return coordinator;
	}
}
