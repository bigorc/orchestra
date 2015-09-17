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
			if(cmd.hasOption("permission")) {
				String[] permissions = cmd.getOptionValues("perm");
				for(String p : permissions) {
					builder.addParameter("permission", p);
				}
			}
		} else if(method.equals("update")) {
			builder.setMethod("put");
			if(cmd.hasOption("permission")) {
				String[] permissions = cmd.getOptionValues("perm");
				for(String p : permissions) {
					builder.addParameter("permission", p);
				}
			}
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		output(response);
	}

}
