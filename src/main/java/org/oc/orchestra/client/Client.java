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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
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
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryOneTime;
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
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.auth.KeystoreHelper;
import org.oc.orchestra.constraint.Constraint;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.coordinate.Curator;
import org.oc.orchestra.coordinate.ResourceWatcher;
import org.oc.orchestra.coordinate.TaskWatcher;
import org.oc.orchestra.parser.ConstraintParser;
import org.oc.orchestra.parser.ConstraintsVisitor;
import org.oc.orchestra.parser.RulesLexer;
import org.oc.orchestra.parser.RulesParser;
import org.oc.orchestra.provider.ACLProvider;
import org.oc.orchestra.resource.Resource;
import org.oc.util.CipherUtil;
import org.oc.util.HttpUtil;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class Client implements Daemon{
	static String resourcePath = "/orchestra/resources";
	static String taskPath = "/orchestra/tasks";
	private static String name = getName();
	protected static String resourceClientPath = resourcePath + "/" + name;
	protected static String taskClientPath = taskPath + "/" + name;

	private static final String keystore_pass = "password";

	private static String connectString;
	private static int zk_session_timeout = 60000;
	private CuratorFramework curator;
	private static String username;

	private static final transient Logger logger = LoggerFactory.getLogger(Client.class);
	
	static String server = "orchestra";
	static int server_port = 8183;
	
	public static String getServer() {
		return server;
	}

	public static void setServer(String server) {
		Client.server = server;
	}

	public static int getServer_port() {
		return server_port;
	}

	public static void setServer_port(int server_port) {
		Client.server_port = server_port;
	}

		
	public static String getConnectString() {
		return connectString;
	}

	public static void setConnectString(String connectString) {
		Client.connectString = connectString;
	}

	public static void setCoordinator(Coordinator coordinator) {
		Client.coordinator = coordinator;
	}

	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		Client.username = username;
	}

	public static String getPassword() {
		return password;
	}

	public static void setPassword(String password) {
		Client.password = password;
	}

	private static String password;

	private static Coordinator coordinator;
	
	private String keystorename = "keystore/clientKey.jks";
	private Timer taskTimer;
	private Timer resourceTimer;

	public Client() {
		if(username == null) config();
		curator = getClientBuilder(false).build();
		curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			boolean needReregister = false;
			
			@Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                logger.info("** STATE CHANGED TO : " + newState);
                if(newState == ConnectionState.LOST) {
                	needReregister = true;
                }
                if(newState == ConnectionState.RECONNECTED) {
                	start();
                }
            }
            
        });
		curator.start();
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
	
	void startTaskWatcher() {
		taskTimer = new Timer();
        taskTimer.schedule(new TaskWatcherTimerTask(curator) , 0, zk_session_timeout);
	}

	protected static String getZkTaskClientPath() {
		if(taskClientPath == null) {
			taskClientPath = taskPath + "/" + getName();
		}
		return taskClientPath;
	}

	protected static void setZkTaskClientPath(String zkTaskClientPath) {
		Client.taskClientPath = zkTaskClientPath;
	}
	
	void startResourcesWatcher() {
		logger.info("Registering resource watcher");
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

	public static String getZkClientResourcePath() throws UnknownHostException {
		if(resourceClientPath == null) {
			resourceClientPath = resourcePath + "/" + getName();
		}
		return resourceClientPath;
	}

	public static void setZkClientResourcePath(String zkClientResourcePath) {
		Client.resourceClientPath = zkClientResourcePath;
	}

	public void createParents() {
		//create the orchestra task and resource path
		CuratorFramework cf = getClientBuilder(false).build();
		cf.start();
		try {
			List<ACL> aclList = new ArrayList<ACL>();
			Id roleId = new Id("role", "admin");
	    	ACL acl = new ACL(Perms.ALL, roleId);
	    	aclList.add(acl);
			if(cf.checkExists().forPath(resourcePath) == null) {
				cf.create().creatingParentsIfNeeded().withACL(aclList).forPath(resourcePath);
			}
			if(cf.checkExists().forPath(taskPath) == null) {
				cf.create().forPath(taskPath);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		cf.close();
		
		cf = getClientBuilder(true).build();
		cf.start();
		try {
			if(cf.checkExists().forPath(getZkClientResourcePath()) == null) {
				cf.create().forPath(getZkClientResourcePath());
			}
			if(cf.checkExists().forPath(getZkTaskClientPath()) == null) {
				cf.create().forPath(getZkTaskClientPath());
			}
		} catch(KeeperException.NodeExistsException e) {
			logger.warn("Client resource or task path already exists.");
		} catch (KeeperException.ConnectionLossException e) {
			createParents();
		} catch (Exception e) {
			e.printStackTrace();
		}
		cf.close();
	}

	public void resetAcl(String clientname) {
		CuratorFramework cf = getClientBuilder(true).build();
		cf.start();
		List<ACL> aclList = getAclList(clientname);
		try {
			cf.setACL().withACL(aclList).forPath(resourcePath + "/" + clientname);
			cf.setACL().withACL(aclList).forPath(taskPath + "/" + clientname);
		} catch (Exception e) {
			e.printStackTrace();
		}
		cf.close();
	}
	

	public List<ACL> getAclList(String clientname) {
		HttpCommand cmd = new HttpCommandBuilder(username , password )
			.setHost(server)
			.setScheme("https")
			.setPort(server_port)
			.setAction("read")
			.setTarget("zkacl")
			.addPathParameter(clientname)
			.build();
		JSONObject json = null;
		HttpResponse response = cmd.execute();
	    if(200 == response.getStatusLine().getStatusCode()) {
	    	try {
				String str = EntityUtils.toString(response.getEntity());
				json = (JSONObject) JSONValue.parse(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	
	    }
	    JSONArray jarr = (JSONArray) json.get("roles");
	    List<ACL> aclList = new ArrayList<ACL>();
	    for(Object obj : jarr) {
	    	String role = (String) ((JSONObject)obj).get("name");
	    	Id roleId = new Id("role", role);
	    	ACL acl = new ACL(Perms.ALL, roleId);
	    	aclList.add(acl);
	    }
		return aclList;
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

	public static Coordinator getCoordinator() {
		if(coordinator == null) {
			coordinator = new Curator();
		}
		return coordinator;
	}
	
	public static Coordinator getCoordinator(String client) {
		coordinator = new Curator(client);
		return coordinator;
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		System.out.println("orchestra client daemon started.");
		config();
		curator = getClientBuilder(true).build();
		curator.start();
	}

	public static void config() {
		Properties conf = new Properties();
        InputStream is;
		try {
			is = new FileInputStream("conf/client.conf");
			conf.load(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		username = conf.getProperty("username");
		password = conf.getProperty("password");
		connectString = conf.containsKey("zookeeper.connectString") ? conf.getProperty("zookeeper.connectString") : connectString;
		zk_session_timeout = conf.containsKey("zookeeper.session.timeout") ? 
			Integer.valueOf(conf.getProperty("zookeeper.session.timeout")) : 
				zk_session_timeout;
	}
	
	public static CuratorFrameworkFactory.Builder getClientBuilder(boolean withAclProvider) {
		// Create a client builder
		if(connectString == null) config();
		CuratorFrameworkFactory.Builder clientBuilder = CuratorFrameworkFactory
				.builder()
				.connectString(connectString)
				.sessionTimeoutMs(zk_session_timeout)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3));
		if(withAclProvider) {
//			clientBuilder.aclProvider(new ACLProvider());
		}
//		clientBuilder.authorization("role", (username + ":" + password).getBytes());
		return clientBuilder;
	}
	
	@Override
	public void start() {
		startResourcesWatcher();
		startTaskWatcher();
	}

	@Override
	public void stop() throws Exception {
		System.out.println("orchestra client daemon stopped.");
		taskTimer.cancel();
		resourceTimer.cancel();
	}

	public void update() {
		resetAcl(getName());
	}
}
