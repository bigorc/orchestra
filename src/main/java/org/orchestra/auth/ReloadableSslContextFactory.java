package org.orchestra.auth;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.restlet.data.Parameter;
import org.restlet.ext.ssl.DefaultSslContextFactory;
import org.restlet.ext.ssl.SslContextFactory;
import org.restlet.ext.ssl.internal.DefaultSslContext;
import org.restlet.util.Series;

public class ReloadableSslContextFactory extends DefaultSslContextFactory {

    private ReloadableX509TrustManager reloadableX509TrustManager;
	public javax.net.ssl.SSLContext createSslContext() throws Exception {
    	javax.net.ssl.SSLContext result = null;
        javax.net.ssl.KeyManagerFactory kmf = null;

        if ((getKeyStorePath() != null) || (getKeyStoreProvider() != null)
                || (getKeyStoreType() != null)) {
            // Loads the key store.
            KeyStore keyStore = (this.getKeyStoreProvider() != null) ? KeyStore
                    .getInstance(
                            (this.getKeyStoreType() != null) ? this.getKeyStoreType()
                                    : KeyStore.getDefaultType(),
                            this.getKeyStoreProvider())
                    : KeyStore
                            .getInstance((this.getKeyStoreType() != null) ? this.getKeyStoreType()
                                    : KeyStore.getDefaultType());
            FileInputStream keyStoreInputStream = null;

            try {
                keyStoreInputStream = ((this.getKeyStorePath() != null) && (!"NONE"
                        .equals(this.getKeyStorePath()))) ? new FileInputStream(
                        this.getKeyStorePath()) : null;
                keyStore.load(keyStoreInputStream, this.getKeyStorePassword());
            } finally {
                if (keyStoreInputStream != null) {
                    keyStoreInputStream.close();
                }
            }

            // Creates the key-manager factory.
            kmf = javax.net.ssl.KeyManagerFactory
                    .getInstance(this.getKeyManagerAlgorithm());
            kmf.init(keyStore, this.getKeyStoreKeyPassword());
        }

        if (this.getTrustStorePath() != null) {
            reloadableX509TrustManager = new ReloadableX509TrustManager(getTrustStorePath());
        }

        // Creates the SSL context
        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext
                .getInstance(this.getProtocol());
        SecureRandom sr = null;

        if (this.getSecureRandomAlgorithm() != null) {
            sr = SecureRandom.getInstance(this.getSecureRandomAlgorithm());
        }

        sslContext.init(kmf != null ? kmf.getKeyManagers() : null,
                new TrustManager[] { reloadableX509TrustManager }, sr);

        // Wraps the SSL context to be able to set cipher suites and other
        // properties after SSL engine creation for example
        result = createWrapper(sslContext);
        return result;
	}
	
	public void reload() {
		try {
			reloadableX509TrustManager.reloadTrustManager();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}