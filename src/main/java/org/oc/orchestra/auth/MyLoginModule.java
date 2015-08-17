package org.oc.orchestra.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class MyLoginModule implements LoginModule {

	@Override
	public boolean abort() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean commit() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void initialize(Subject arg0, CallbackHandler arg1,
			Map<String, ?> arg2, Map<String, ?> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean login() throws LoginException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

}
