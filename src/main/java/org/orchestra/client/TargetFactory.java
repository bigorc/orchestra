package org.orchestra.client;

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
		} else if(targetName.equals("ro")) {
			target = new RoTarget(builder);
		} else if(targetName.equals("role")) {
			target = new RoleTarget(builder);
		} else if(targetName.equals("userrole")) {
			target = new UserRoleTarget(builder);
		}
		return target;
	}
}
