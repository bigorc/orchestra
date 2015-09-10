package org.oc.orchestra.provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.ServerCnxn;
import org.apache.zookeeper.server.auth.AuthenticationProvider;
import org.oc.orchestra.client.HttpCommand;
import org.oc.orchestra.client.HttpCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleAuthenticationProvider implements AuthenticationProvider {
	private final Logger logger = LoggerFactory.getLogger(RoleAuthenticationProvider.class);
	private String host = "orchestra";
	private int port = 8183;
 
	public String getScheme() {
		return "role";
	}

	public Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
		loadConfig();
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
			.setTarget("zkauth")
			.setNeedAuthHeader(true)
			.build();
	    HttpResponse response = cmd.execute();
	    if(200 == response.getStatusLine().getStatusCode()) {
	    	cnxn.addAuthInfo(new Id(getScheme(), id));
	        return Code.OK;
	    }
	    return Code.AUTHFAILED;
	}
 
	private void loadConfig() {
		Properties conf = new Properties();
		try {
			conf.load(RoleAuthenticationProvider.class.getResourceAsStream("zoo.cfg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		host = conf.containsKey("orchestra.server") ? conf.getProperty("orchestra.server") : host;
		port = conf.containsKey("orchestra.port") ? Integer.valueOf(conf.getProperty("orchestra.port")) : port;
	}

	public boolean matches(String id, String aclExpr) {
		loadConfig();
	    System.out.println("id:" + id + ";" + "acl:" + aclExpr);
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
			.setTarget("zkauth")
			.addPathParameter(role)
			.setNeedAuthHeader(true)
			.build();
	    HttpResponse response = cmd.execute();
	    if(response.getStatusLine().getStatusCode() == 200) return true;
	    return false;
	}

	public boolean isAuthenticated() {
	    return true;
	}

	public boolean isValid(String id) {
		return true;
	}
}
