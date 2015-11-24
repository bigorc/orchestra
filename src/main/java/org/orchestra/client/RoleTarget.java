package org.orchestra.client;

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
			String[] permissions = cmd.getOptionValues("perm");
			for(String p : permissions) {
				builder.addParameter("permission", p);
			}
			if(cmd.hasOption("a")) {
				builder.addParameter("add", "true");
				if(cmd.hasOption("rm"))
					throw new RuntimeException("Can't add and remove permissions at the same time.");
			}
			if(cmd.hasOption("rm")) {
				builder.addParameter("remove", "true");
			}
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		output(response);
	}

}
