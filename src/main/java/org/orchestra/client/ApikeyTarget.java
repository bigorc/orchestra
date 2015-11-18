package org.orchestra.client;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class ApikeyTarget extends Target {
	public ApikeyTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	public ApikeyTarget() {
		this.builder = new HttpCommandBuilder(Client.getUsername(), Client.getPassword())
			.setScheme("https")
			.setHost(Client.getProperty("server"))
			.setPort(Integer.valueOf(Client.getProperty("port")));
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		String username = builder.getUsername();
		String password = builder.getPassword();
		String clientname = Client.getName();
		ClientAuthHelper helper = new ClientAuthHelper(username, password);
		builder.setTarget("apikey").setNeedAuthHeader(true)
			.addPathParameter(username).addPathParameter(clientname);
		if(method.equals("delete")) {
			builder.setMethod("delete");
		} else if(method.equals("get")) {
			builder.setMethod("get");
		} else if(method.equals("create")) {
			builder.setMethod("post");
		} else if(method.equals("update")) {
			builder.setMethod("put");
		}
		HttpCommand command = builder.build();
		HttpResponse response = command.execute();
		if(method.equals("delete")) {
			helper.removeApikeyFile();
		} else {
			helper.saveApikeyToFile(response);
		}
//		output(response);
	}

}
