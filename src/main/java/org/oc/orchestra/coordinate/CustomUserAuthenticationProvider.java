/*
 * Copyright 2012 Michael Morello
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.oc.orchestra.coordinate;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.ServerCnxn;
import org.apache.zookeeper.server.auth.AuthenticationProvider;
import org.oc.orchestra.client.HttpCommand;
import org.oc.orchestra.client.HttpCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomUserAuthenticationProvider implements AuthenticationProvider {
	private final Logger logger = LoggerFactory.getLogger(CustomUserAuthenticationProvider.class);
	private String host = "orchestra";
	private int port = 8183;
 
	public String getScheme() {
		return "role";
	}

	public Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
		final String id = new String(authData, StandardCharsets.UTF_8);
	    // A non null or empty user name must be provided
	    logger.info(id);
	    String[] idarr = id.split(":");
	    String username = idarr[0];
	    String password = idarr[1];
	    HttpCommand cmd = new HttpCommandBuilder(username, password)
			.setHost(host )
			.setScheme("https")
			.setPort(port)
			.setAction("read")
			.setTarget("apikey")
			.addPathParameter(username)
			.build();
	    HttpResponse response = cmd.execute();
	    if(200 == response.getStatusLine().getStatusCode()) {
	    	cnxn.addAuthInfo(new Id(getScheme(), id));
	        return Code.OK;
	    }
	    return Code.AUTHFAILED;
	}
 
	public boolean matches(String id, String aclExpr) {
	    logger.info("id:" + id + ";" + "acl:" + aclExpr);
	    String[] idarr = id.split(":");
	    String username = idarr[0];
	    String password = idarr[1];
	    String[] aclarr = aclExpr.split(":");
	    String role = aclarr[0];
	    HttpCommand cmd = new HttpCommandBuilder(username, password)
			.setHost(host )
			.setScheme("https")
			.setPort(port)
			.setAction("read")
			.setTarget("userrole")
			.addPathParameter(username)
			.addPathParameter(role)
			.build();
	    HttpResponse response = cmd.execute();
	    if(response.getStatusLine().getStatusCode() == 204) return true;
	    return false;
	}

	public boolean isAuthenticated() {
	    return true;
	}

	public boolean isValid(String id) {
		// A valid user name is at least 1 char length
		return true;
	}
}
