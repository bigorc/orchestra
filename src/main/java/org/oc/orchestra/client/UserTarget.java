package org.oc.orchestra.client;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class UserTarget extends Target {

	public UserTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		String user = null;
		if(cmd.getArgs().length > 2) {
			user = cmd.getArgs()[2];
		}
		builder.setTarget("user").addPathParameter(user);
		if(method.equals("list")) {
			builder.setMethod("get").setTarget("users");
		} else if(method.equals("delete")) {
			builder.setMethod("delete");
		} else if(method.equals("get")) {
			builder.setMethod("get");
		} else if(method.equals("create")) {
			builder.setMethod("post");
		} else if(method.equals("update")) {
			if(!cmd.hasOption("up")) ArgsHelper.usage();
			String user_pass = cmd.getOptionValue("up");
			builder.setMethod("put").setParameter("password", user_pass);
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		output(response);
	}

}
