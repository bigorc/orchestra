package org.oc.orchestra.client;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class RoleTarget extends Target {

	public RoleTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		String role = null;
		if(cmd.getArgs().length > 2) {
			role = cmd.getArgs()[2];
		}
		builder.setTarget("role").addPathParameter(role);
		if(method.equals("list")) {
			builder.setMethod("get").setTarget("roles");
		} else if(method.equals("delete")) {
			builder.setMethod("delete");
		} else if(method.equals("get")) {
			builder.setMethod("get");
		} else if(method.equals("create")) {
			builder.setMethod("post");
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		output(response);
	}

}
