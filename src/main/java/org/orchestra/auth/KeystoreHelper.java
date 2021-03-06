package org.orchestra.auth;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreHelper {
	private static final transient Logger logger = LoggerFactory.getLogger(KeystoreHelper.class);
	private String keystorename;
	private KeyStore keystore;
	private String password;
	FileInputStream input;
	
	public KeyStore getKeystore() {
		return keystore;
	}
	
	public KeystoreHelper(String keystorename, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		this.keystorename = keystorename;
		this.password = password;
		if(keystore == null) keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(null , password.toCharArray());
		if(!new File(keystorename).exists()) {
			store();
		}
		load();
		logger.info("Using keystore-file : "+keystorename);
	}
	
	public void reload(String keystorename, String password) throws NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyStoreException {
		this.keystorename = keystorename;
		this.password = password;
		if(!new File(keystorename).exists()) {
			store();
		}
		load();
		logger.info("Using keystore-file : "+keystorename);
	}
	
	public void saveCertificate(String alias, Certificate cert ) throws KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		keystore.setCertificateEntry(alias, cert);
	    store();
	}

	private void load() throws FileNotFoundException, KeyStoreException,
			IOException, NoSuchAlgorithmException, CertificateException {
		FileInputStream input = new FileInputStream(keystorename);
		keystore.load(input, password.toCharArray());
		input.close();
	}
	
	private void store() throws FileNotFoundException, KeyStoreException,
			IOException, NoSuchAlgorithmException, CertificateException {
		File file = new File(keystorename);
		if(file.getParentFile() != null) file.getParentFile().mkdirs();
		FileOutputStream output = new FileOutputStream(file);
	    keystore.store(output, password.toCharArray());
	    output.flush();
	    output.close();
	}
	
	public Key loadKey(String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, FileNotFoundException, CertificateException, IOException {
		load();
		return keystore.getKey(alias, password.toCharArray());
	}
	
	public X509Certificate loadCertificate(String alias) throws KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException, IOException {
		load();
		return (X509Certificate) keystore.getCertificate(alias);
	}
	
	public void savePrivateKey(String encodedPrivateKey,
			String alias, String key_password, 
			String encodeCert) throws KeyStoreException, IOException,
			NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, InvalidKeySpecException {

		byte[] key = Base64.decode(encodedPrivateKey);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec ( key );
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate (keySpec);
        
        byte[] cert = Base64.decode(encodeCert);
        // loading CertificateChain
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certstream = new ByteArrayInputStream(cert);
		Collection<?> clientCerts = cf.generateCertificates(certstream ) ;
        Certificate[] certs = (Certificate[]) clientCerts.toArray(new Certificate[0]);

        if(key_password != null) password = key_password;
        // storing keystore
        keystore.setKeyEntry(alias, privateKey, 
                       password.toCharArray(),
                       certs );
        logger.info("Key and certificate stored.");
        logger.info("Alias:" + alias);
        store();
	}

	public void deleteCertificate(String alias) throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		keystore.deleteEntry(alias);
		store();
	}

	public boolean containsCertificate(String alias) throws KeyStoreException {
		return keystore.containsAlias(alias);
	}
}
