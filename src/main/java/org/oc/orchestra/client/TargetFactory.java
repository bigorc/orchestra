package org.oc.orchestra.client;

public class TargetFactory {

	private HttpCommandBuilder builder;

	public TargetFactory(HttpCommandBuilder commandBuilder) {
		this.builder = commandBuilder;
	}

	public Target getTarget(String targetName) {
		Target target = null;
		if(targetName.equals("client")) {
			target = new ClientTarget(builder);
		} else if(targetName.equals("apikey")) {
			target = new ApikeyTarget(builder);
		} else if(targetName.equals("user")) {
			target = new UserTarget(builder);
		}
		return target;
	}
}
