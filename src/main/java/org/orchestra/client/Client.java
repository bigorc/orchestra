package org.orchestra.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import org.apache.commons.cli.ParseException;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.orchestra.coordinate.Coordinator;
import org.orchestra.coordinate.Curator;
import org.orchestra.coordinate.ResourceWatcher;
import org.orchestra.coordinate.TaskWatcher;
import org.orchestra.provider.ACLProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements Daemon{
	static String resourcePath = "/orchestra/resources";
	static String taskPath = "/orchestra/tasks";
	private static String name = getName();
	protected static String resourceClientPath = resourcePath + "/" + name;
	protected static String taskClientPath = taskPath + "/" + name;

	private static int zk_session_timeout = 60000;
	private CuratorFramework curator;
	private static String username;

	private static final transient Logger logger = LoggerFactory.getLogger(Client.class);
	
	static Map<String, String> properties = new HashMap<String, String>();
	static {
		properties.put("port", "8183");
		properties.put("zookeeper.connectString", "localhost:2281");
		properties.put("apikey.dir", ".");
	}
	
	public static String getProperty(String key) {
		return properties.get(key);
	}
	
	public static void setProperty(String key, String value) {
		properties.put(key, value);
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
	
	private Timer taskTimer;
	private Timer resourceTimer;

	public Client() {
		if(username == null) config();
		curator = getClientBuilder(false).build();
		curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			@Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                logger.info("** STATE CHANGED TO : " + newState);
                if(newState == ConnectionState.LOST) {
                }
                if(newState == ConnectionState.RECONNECTED) {
                	start();
                }
            }
            
        });
	}
	
	public static void main(String[] args) throws ParseException, ClientProtocolException, IOException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, URISyntaxException, SignatureException {
		new ArgsHelper().handle(args);
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

	void startTaskWatcher() {
		logger.info("Registering task watcher");
		TaskWatcher watcher = new TaskWatcher(curator, taskClientPath);
		try {
			List<String> children = curator.getChildren().usingWatcher(watcher)
					.forPath(taskClientPath);
			watcher.handleTasks(children);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	void startResourcesWatcher() {
		logger.info("Registering resource watcher");
		try {
			ResourceWatcher watcher = new ResourceWatcher(curator, resourceClientPath);
			List<String> children = curator.getChildren().usingWatcher(
					watcher).forPath(resourceClientPath);
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
	
	public void deleteParents() {
		CuratorFramework cf = getClientBuilder(true).build();
		cf.start();
		try {
			if(cf.checkExists().forPath(getZkClientResourcePath()) != null) {
				cf.delete().forPath(getZkClientResourcePath());
			}
			if(cf.checkExists().forPath(getZkTaskClientPath()) != null) {
				cf.delete().forPath(getZkTaskClientPath());
			}
		} catch(KeeperException.NoNodeException e) {
			logger.warn("Client resource or task path does not exists.");
		} catch (KeeperException.ConnectionLossException e) {
			deleteParents();
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
			.setHost(getProperty("server"))
			.setScheme("https")
			.setPort(Integer.valueOf(getProperty("port")))
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
	    } else {
	    	logger.error(response.getStatusLine().getReasonPhrase());
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
		curator = getClientBuilder(false).build();
		curator.start();
	}

	public static void config() {
		Properties conf = new Properties();
        InputStream is = ClassLoader.getSystemResourceAsStream("client.conf");
		try {
			conf.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(String key : conf.stringPropertyNames()) {
			properties.put(key, conf.getProperty(key));
		}
		username = conf.getProperty("username");
		password = conf.getProperty("password");
		properties.put("server", conf.getProperty("server"));
		if(conf.containsKey("port")) {
			properties.put("port", conf.getProperty("port"));
		}
		if(conf.containsKey("zookeeper.connectString")) {
			properties.put("zookeeper.connectString", conf.getProperty("zookeeper.connectString"));
		}
		zk_session_timeout = conf.containsKey("zookeeper.session.timeout") ? 
			Integer.valueOf(conf.getProperty("zookeeper.session.timeout")) : 
				zk_session_timeout;
		logger.debug("username = " + username);
		logger.debug("server=" + properties.get("server"));
		logger.debug("port=" + properties.get("port"));
		logger.debug("zookeeper.connectString = " + properties.get("zookeeper.connectString"));
		logger.debug("zk_session_timeout = " + zk_session_timeout);
		logger.debug("apikey.dir=" + properties.get("apikey.dir"));
		if(conf.containsKey("keystore")) {
			System.setProperty("javax.net.ssl.keyStore", conf.getProperty("keystore"));
		} else {
			System.setProperty("javax.net.ssl.keyStore", "keystore/clientKey.jks");
		}
		logger.debug("javax.net.ssl.keyStore = " + System.getProperty("javax.net.ssl.keyStore"));
		if(conf.containsKey("keystore.password")) {
			System.setProperty("javax.net.ssl.keyStorePassword", conf.getProperty("keystore.password"));
		} else {
			System.setProperty("javax.net.ssl.keyStorePassword", "password");
		}
		logger.debug("javax.net.ssl.keyStorePassword = " + System.getProperty("javax.net.ssl.keyStorePassword"));
		if(conf.containsKey("truststore")) {
			System.setProperty("javax.net.ssl.trustStore", conf.getProperty("truststore"));
		} else {
			System.setProperty("javax.net.ssl.trustStore", "keystore/clientTrust.jks");
		}
		logger.debug("javax.net.ssl.trustStore = " + System.getProperty("javax.net.ssl.trustStore"));
		if(conf.containsKey("truststore.password")) {
			System.setProperty("javax.net.ssl.trustStorePassword", conf.getProperty("truststore.password"));
		} else {
			System.setProperty("javax.net.ssl.trustStorePassword", "password");
		}
		logger.debug("javax.net.ssl.trustStorePassword = " + System.getProperty("javax.net.ssl.trustStorePassword"));
		if(conf.containsKey("zookeeper.keystore")) {
			System.setProperty("zookeeper.ssl.keyStore.location", conf.getProperty("zookeeper.keystore"));
		} else {
			System.setProperty("zookeeper.ssl.keyStore.location", "keystore/testKeyStore.jks");
		}
		logger.debug("zookeeper.ssl.keyStore.location = " + System.getProperty("zookeeper.ssl.keyStore.location"));
		if(conf.containsKey("zookeeper.keystore.password")) {
			System.setProperty("zookeeper.ssl.keyStore.password", conf.getProperty("zookeeper.keystore.password"));
		} else {
			System.setProperty("zookeeper.ssl.keyStore.password", "testpass");
		}
		logger.debug("zookeeper.ssl.keyStore.password = " + System.getProperty("zookeeper.ssl.keyStore.password"));
		if(conf.containsKey("zookeeper.truststore")) {
			System.setProperty("zookeeper.ssl.trustStore.location", conf.getProperty("zookeeper.truststore"));
		} else {
			System.setProperty("zookeeper.ssl.trustStore.location", "keystore/testTrustStore.jks");
		}
		logger.debug("zookeeper.ssl.trustStore.location = " + System.getProperty("zookeeper.ssl.trustStore.location"));
		if(conf.containsKey("zookeeper.truststore.password")) {
			System.setProperty("zookeeper.ssl.trustStore.password", conf.getProperty("zookeeper.truststore.password"));
		} else {
			System.setProperty("zookeeper.ssl.trustStore.password", "testpass");
		}
		logger.debug("zookeeper.ssl.trustStore.password = " + System.getProperty("zookeeper.ssl.trustStore.password"));
		if(conf.containsKey("zookeeper.clientCnxnSocket")) {
			System.setProperty("zookeeper.clientCnxnSocket", conf.getProperty("zookeeper.clientCnxnSocket"));
		} else {
			System.setProperty("zookeeper.clientCnxnSocket", "org.apache.zookeeper.ClientCnxnSocketNetty");
		}
		logger.debug("zookeeper.clientCnxnSocket = " + System.getProperty("zookeeper.clientCnxnSocket"));
		System.setProperty("zookeeper.client.secure", "true");
		logger.debug("zookeeper.client.secure = " + System.getProperty("zookeeper.client.secure"));
		
	}
	
	public static CuratorFrameworkFactory.Builder getClientBuilder(boolean withAclProvider) {
		// Create a client builder
		CuratorFrameworkFactory.Builder clientBuilder = CuratorFrameworkFactory
				.builder()
				.connectString(properties.get("zookeeper.connectString"))
				.sessionTimeoutMs(zk_session_timeout)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3));
		if(withAclProvider) {
			clientBuilder.aclProvider(new ACLProvider());
		}
		clientBuilder.authorization("role", (username + ":" + password).getBytes());
		return clientBuilder;
	}
	
	@Override
	public void start() {
		if(curator.getState() != CuratorFrameworkState.STARTED) {
			curator.start();
		}
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
