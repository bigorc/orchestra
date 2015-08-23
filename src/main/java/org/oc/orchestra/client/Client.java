package org.oc.orchestra.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.codec.Base64;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.auth.KeystoreHelper;
import org.oc.orchestra.constraint.Constraint;
import org.oc.orchestra.coordinate.Curator;
import org.oc.orchestra.coordinate.ResourceWatcher;
import org.oc.orchestra.coordinate.TaskWatcher;
import org.oc.orchestra.parser.ConstraintParser;
import org.oc.orchestra.parser.ConstraintsVisitor;
import org.oc.orchestra.parser.RulesLexer;
import org.oc.orchestra.parser.RulesParser;
import org.oc.orchestra.resource.Resource;
import org.oc.util.CipherUtil;
import org.oc.util.HttpUtil;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class Client {
	private static final transient Logger logger = LoggerFactory.getLogger(Client.class);
	
	static String host = "orchestra";
	static int port = 8183;
	
	private static String name;
	protected static String zkClientResourcePath;
	protected static String zkTaskClientPath;

	private static final String keystore_pass = "password";

	private String connectString;
	private CuratorFramework curator;
	private String username;
	private String password;
	private String keystorename = "keystore/clientKey.jks";

	public Client(String connectString) {
		this.connectString = connectString;
	}

	public Client(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public static void main(String[] args) throws ParseException, ClientProtocolException, IOException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, URISyntaxException, SignatureException {
		String filename = null;
		new ArgsHelper().handle(args);

//	    httpclient.getConnectionManager().shutdown();
		/*
		System.out.println(filename);
		List<Resource> resources = ResourceFactory.makeResources(filename);
		for(Resource r: resources) {
			r.realize();
		}*/
	}


	public void start() {
		createParents();
		startResourcesWatcher();
		startTaskWatcher();
	}
	
	void startTaskWatcher() {
		new Thread() {
			public void run() {
				String taskClientPath = Client.getZkTaskClientPath();
				TaskWatcher watcher = new TaskWatcher(curator, taskClientPath);
				List<String> children;
				try {
					children = curator.getChildren().usingWatcher(
							watcher).forPath(taskClientPath);
					//tasks children might be added before watchers are registered
					//that's handled here
					watcher.handleTasks(children);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	protected static String getZkTaskClientPath() {
		if(zkTaskClientPath == null) {
			zkTaskClientPath = "/orchestra/tasks/" + getName();
		}
		return zkTaskClientPath;
	}

	protected static void setZkTaskClientPath(String zkTaskClientPath) {
		Client.zkTaskClientPath = zkTaskClientPath;
	}
	
	void startResourcesWatcher() {
		
		new Thread() {
			public void run() {
				try {
					String clientResourcePath = Client.getZkClientResourcePath();
					ResourceWatcher watcher = new ResourceWatcher(curator, clientResourcePath);
					List<String> children = curator.getChildren().usingWatcher(
							watcher).forPath(clientResourcePath);
					//read task children might be added before watchers are registered
					//that's handled here
					watcher.handleResources(children);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static String getZkClientResourcePath() throws UnknownHostException {
		if(zkClientResourcePath == null) {
			zkClientResourcePath = "/orchestra/resources/" + getName();
		}
		return zkClientResourcePath;
	}

	public static void setZkClientResourcePath(String zkClientResourcePath) {
		Client.zkClientResourcePath = zkClientResourcePath;
	}

	public void createParents() {
		RetryPolicy retryPolicy =  new ExponentialBackoffRetry(1000 , 3);
		curator = CuratorFrameworkFactory.newClient(connectString, retryPolicy );
		curator.start();
		String resourcePath = null;
		String taskPath = null;
		try {
			resourcePath = getZkClientResourcePath();
			taskPath = getZkTaskClientPath();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			logger.error("Unable to get client name");
			System.exit(1);
		}
		try {
			curator.create().creatingParentsIfNeeded().forPath(resourcePath);
			curator.create().creatingParentsIfNeeded().forPath(taskPath);
		} catch(KeeperException.NodeExistsException e) {
			logger.warn("Client resource or task path already exists.");
		} catch (KeeperException.ConnectionLossException e) {
			createParents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setName(String name) {
		Client.name = name;
	}
	
	public static String getName() {
		if(name == null) {
			try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				logger.error("Unable to get host name");
				e.printStackTrace();
				System.exit(1);
			}
		}
		return name;
	}
	
}
