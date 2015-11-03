package org.orchestra.auth.shiro;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.orchestra.rest.ServerAuthHelper;
import org.restlet.Request;

public class RequestSignatureMatcher extends PasswordMatcher {
	public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
		if(token instanceof RequestToken) {
			Request request = ((RequestToken) token).getRequest();
			ServerAuthHelper helper = new ServerAuthHelper(request);
			try {
				return helper.validate();
			} catch (UnrecoverableKeyException | SignatureException
					| KeyStoreException | NoSuchAlgorithmException
					| CertificateException | IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return super.doCredentialsMatch(token, info);
		}
	}
}
