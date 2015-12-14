package org.orchestra.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.crypto.BlowfishCipherService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.ByteSource;
import org.apache.shiro.util.Factory;
import org.apache.shiro.util.SimpleByteSource;

public class CipherUtil {
	private static final String CIPHER_ALGORITHM = "Blowfish";
	private static final String ALGORITHM = "HmacSHA256";
	public static String encrypt(String secret, Key key) {
		BlowfishCipherService cipher = new BlowfishCipherService();
		byte[] keyBytes = key.getEncoded();
		byte[] secretBytes = CodecSupport.toBytes(secret);
		String encrypted = cipher.encrypt(secretBytes, keyBytes).toBase64();
		return encrypted;
	}

	public static String decrypt(String encryptedSecret, Key key) {
		BlowfishCipherService cipher = new BlowfishCipherService();
		byte[] keyBytes = key.getEncoded();
		
		ByteSource decrypted = cipher.decrypt(Base64.decode(encryptedSecret), keyBytes);
		String secret = CodecSupport.toString(decrypted.getBytes());
		return secret;
	}

	public static String decrypt(InputStream in, String password) {
		BlowfishCipherService cipher = new BlowfishCipherService();
		byte[] decodedKey = password.getBytes();
		Key key = new SecretKeySpec(decodedKey, 0, decodedKey.length, CIPHER_ALGORITHM);
		byte[] keyBytes = key.getEncoded();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		cipher.decrypt(in, out, keyBytes);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}
	
	public static void encrypt(String secret, OutputStream out, String password) {
		BlowfishCipherService cipher = new BlowfishCipherService();
		byte[] decodedKey = password.getBytes();
		Key key = new SecretKeySpec(decodedKey, 0, decodedKey.length, CIPHER_ALGORITHM);
		byte[] keyBytes = key.getEncoded();
		InputStream in = new ByteArrayInputStream(secret.getBytes(StandardCharsets.UTF_8));
		cipher.encrypt(in, out, keyBytes);
	}
	
	
	public static byte[] hash(String text) throws SignatureException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(text.getBytes(StandardCharsets.UTF_8));
			return md.digest();
		} catch (Exception e) {
			throw new SignatureException("Unable to compute hash while signing request.", e);
		}
	}

	public static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for (int i = 0; i < data.length; i++) {
			String hex = Integer.toHexString(data[i]);
			if (hex.length() == 1) {
				// Append leading zero.
				sb.append("0");
			} else if (hex.length() == 8) {
				// 	Remove ff prefix from negative numbers.
				hex = hex.substring(6);
			}
			sb.append(hex);
		}
		return sb.toString().toLowerCase(Locale.getDefault());
	}

	public static byte[] sign(String data, byte[] key) throws SignatureException {
		return sign(data.getBytes(StandardCharsets.UTF_8), key);
	}
	
	public static byte[] sign(String data, String key) throws SignatureException {
		return sign(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
	}
	
	public static byte[] sign(byte[] data, byte[] key) throws SignatureException {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(key, ALGORITHM));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new SignatureException("Unable to calculate a request signature: " + e.getMessage(), e);
		}
	}

	public static Key getSecKey() throws KeyStoreException, FileNotFoundException,
			IOException, NoSuchAlgorithmException, CertificateException,
			UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("JCEKS");
		InputStream readStream = new FileInputStream("/root/keystore/serverSec.jks");
		ks.load(readStream, "password".toCharArray());
		Key key = ks.getKey("seckey", "password".toCharArray());
		readStream.close();
		return key;
	}

	public static String encryptPassword(String password) {
		PasswordService passwordService = null;
		Factory<org.apache.shiro.mgt.SecurityManager> factory = 
				(Factory<SecurityManager>) SpringUtil.getBean("securityManager");

		RealmSecurityManager secManager = (RealmSecurityManager)factory.getInstance();
//		RealmSecurityManager secManager = (RealmSecurityManager) SecurityUtils.getSecurityManager();
		for(Realm realm : secManager.getRealms()) {
			if(realm.getName().equals("userRealm")) {
				CredentialsMatcher matcher = ((AuthenticatingRealm)realm).getCredentialsMatcher();
				passwordService = ((PasswordMatcher)matcher).getPasswordService();
			}
		}
		
		String encryptedPassword = passwordService.encryptPassword(password);
		System.out.println(encryptedPassword);
		return encryptedPassword;
	}
	
	public static String generatePassword(String password) {
		DefaultHashService hashService = new DefaultHashService();
		hashService.setHashIterations(500000); 
		hashService.setHashAlgorithmName(Sha256Hash.ALGORITHM_NAME);
		hashService.setGeneratePublicSalt(true);
		hashService.setPrivateSalt(new SimpleByteSource("myVERYSECRETBase64EncodedSalt"));

		DefaultPasswordService passwordService = new DefaultPasswordService();
		passwordService.setHashService(hashService);
		String encryptedPassword = passwordService.encryptPassword(password);
		System.out.println("Result:"+encryptedPassword);
		return encryptedPassword;
	}
}
