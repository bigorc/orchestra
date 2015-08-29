package org.oc.orchestra.rest;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class OrchestraApplication extends Application {
	
	@Override
	public Restlet createInboundRoot(){
		Router router = new Router(getContext());
		router.attach("/ro", RO.class);
		router.attach("/users", Users.class);
		router.attach("/user/{username}", User.class);
		router.attach("/role/{rolename}", Role.class);
		router.attach("/userrole/{username}/{rolename}", UserRole.class);
		router.attach("/apikey/{username}/{clientname}", Apikey.class);
		router.attach("/client/{clientname}", Client.class);
		router.attach("/clients", Clients.class);
		router.attach("/certificate/{clientname}", Client.class);
		
		AuthFilter shiro = new AuthFilter();
		shiro.setNext(router);
		return shiro;
	}
}
