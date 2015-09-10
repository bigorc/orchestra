package org.oc.orchestra.client;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class UserRoleTarget extends Target {

	public UserRoleTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		String user = null;
		String role = null;
		if(cmd.getArgs().length > 3) {
			user = cmd.getArgs()[2];
			role = cmd.getArgs()[3];
		}
		builder.setTarget("userrole").addPathParameter(user).addPathParameter(role);
		if(method.equals("rm")) {
			builder.setMethod("delete");
		} else if(method.equals("get")) {
			builder.setMethod("get");
		} else if(method.equals("add")) {
			builder.setMethod("post");
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		output(response);
	}

}
