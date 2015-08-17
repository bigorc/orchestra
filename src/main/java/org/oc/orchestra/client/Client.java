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
//	public static String server_url =  "https://simpson.org:8183/";
	private static final transient Logger logger = LoggerFactory.getLogger(Client.class);
	
	static String host = "orchestra";
	static int port = 8183;
	private static String name;
	protected static String zkClientResourcePath;
	protected static String zkTaskClientPath;
	private static final String PRIVATE_KEY = "privateKey";
	private static final String CERT = "cert";

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
		Options options = getOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(options, args);
		HttpRequestBase request = null;
        
	    if(args.length == 0) {
	    	startCli();
	    }
	    String username;
		if(cmd.hasOption('u')) {
        	username = cmd.getOptionValue('u');
        } else {
        	username = prompt("username:");
        }
        
        //set password
        String password;
        if(cmd.hasOption('p')) {
        	password = cmd.getOptionValue('p');
        } else {
        	password = prompt("password:");
        }
        
        String zk_connectString;
        if(cmd.hasOption('z')) {
        	zk_connectString = cmd.getOptionValue('z');
        } else {
        	
        }
        
        
		for(String arg : cmd.getArgs()) {
			System.out.println(arg);
		}
		String [] targets = cmd.getArgs();
		HttpCommandBuilder commandBuilder = new HttpCommandBuilder(username, password);
		commandBuilder.setScheme("https").setHost(host).setPort(port);
		if(targets.length < 2) {
			usage();
			System.exit(0);
		} else {
			commandBuilder.setTarget(targets[0]).setAction(targets[1]);
			if(!targets[0].equals("apikey")) {
				
				
				if(targets[0].equals("user")) {
					commandBuilder.setTarget("user");
					String name = null;
					String pass = null;
					if(cmd.hasOption('P')) {
						String[] params = cmd.getOptionValue('P').split(";");
						for(String p : params) {
							String[] kv = p.split("=");
							if(kv[0].equals("name")) name = kv[1];
							if(kv[0].equals("password")) pass = kv[1];
						}
					}
					commandBuilder.addPathParameter(name);
					if(targets[1].equals("create")) {
						if(name == null) name = prompt("name:");
						
						if(pass == null) pass = prompt("password:");
						commandBuilder.setParameter("password", pass);
						request = new HttpPost();
					}
					if(targets[1].equals("update")) {
						if(name == null) name = prompt("name:");

						if(pass == null) pass = prompt("password:");
						System.out.println(pass + " " + pass.getBytes());
						commandBuilder.setParameter("password", pass);
						request = new HttpPut();
					}
					if(targets[1].equals("read")) {
						if(name == null) name = prompt("name:");
						request = new HttpGet();
					}
					if(targets[1].equals("delete")) {
						if(name == null) name = prompt("name:");
						request = new HttpDelete();
					}
					if(targets[1].equals("list")) {
						request = new HttpGet();
					}
				}
				
			}
		}
		
		if(cmd.hasOption("U")) {
			filename = cmd.getOptionValue("U");
			System.out.println(filename);

//			httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			commandBuilder.setTarget("pro");
			
//			HttpPost httppost = new HttpPost(uriBuilder.build());
			commandBuilder.setMethod("post");
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			File file = new File(filename);
			builder.addBinaryBody("field1", file, ContentType.APPLICATION_OCTET_STREAM, filename);
			
			HttpEntity multipart = builder.build();

		    commandBuilder.setEntity(multipart);
		    System.out.println("executing request " + commandBuilder.getRequest().getRequestLine());

		}
		
		HeaderIterator it = request.headerIterator();
		HttpCommand command = commandBuilder.build();
//		request.addHeader("Content-Type", "text/html; charset=UTF-8");
		HttpResponse response = command.execute();
	    HttpEntity resEntity = response.getEntity();

	    System.out.println(response.getStatusLine());
	    if (resEntity != null) {
	    	String str = EntityUtils.toString(resEntity);
			Object json = JSONValue.parse(str);
	    	if(json != null) {
	    		System.out.println(JSONValue.toJSONString(json));
	    	} else {
	    		System.out.println(str);
	    	}
	    }
	    if (resEntity != null) {
	      resEntity.consumeContent();
	    }

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

	private static void startCli() {
		String username = prompt("username:");
		String password = prompt("password:");
		String rule = prompt(">");
		while(!rule.equals("exit")) {
			System.out.println(rule);
			Constraint cons = ConstraintParser.parse(rule + "\n");
			cons.enforce();
			rule = prompt(">");
		}
	}


	private static void usage() {
		
	}


	private static String prompt(String output) {
		System.out.print(output);
		Scanner input = new Scanner(System.in, "utf-8");
		
		return input.nextLine();
	}

	private static Options getOptions() {
		Options options = new Options();
		Option upload   = OptionBuilder.withArgName( "file" )
                .hasArg()
                .withDescription("Upload parameterized resource object")
                .withLongOpt("upload")
                .create('U');
		Option user = OptionBuilder.withArgName( "name" )
                .hasArg()
                .withDescription("User name")
                .withLongOpt("user")
                .create('u');
		Option password = OptionBuilder.withArgName( "password" )
                .hasArg()
                .withDescription("password")
                .withLongOpt("password")
                .create('p');
		Option parameter = OptionBuilder.withArgName( "parameter" )
                .hasArg()
                .withDescription("Parameter")
                .withLongOpt("patameter")
                .create('P');
		
		options.addOption(upload);
		options.addOption(user);
		options.addOption(password);
		options.addOption(parameter);
		return options;
	}

	public void createClient() throws IllegalStateException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeySpecException {
		HttpCommandBuilder builder = new HttpCommandBuilder(username, password);
		String clientname = getName();
		HttpCommand cmd = builder
			.setHost(host )
			.setScheme("https")
			.setPort(port)
			.setAction("create")
			.setTarget("client")
			.addPathParameter(clientname )
			.setParameter("clientname", clientname)
			.build();
		HttpResponse response = cmd.execute();
		if(response.getStatusLine().getStatusCode() == 409) {
			throw new RuntimeException("Client already exists.");
		} else if(response.getStatusLine().getStatusCode() == 201) {
			JSONObject json = (JSONObject) JSONValue.parse(
					new InputStreamReader(response.getEntity().getContent()));
			System.out.println(json.toString());

			String encodedPk = (String) json.get(PRIVATE_KEY);

			// initializing and clearing keystore 
			String alias = clientname;
			String encodeCert = (String) json.get(CERT);
			
			KeystoreHelper helper = new KeystoreHelper(keystorename, keystore_pass);
			helper.savePrivateKey(encodedPk, alias, keystore_pass, encodeCert);
		}
	}
	
	public void deleteClient() throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		String clientname = getName();
		HttpCommand cmd = new HttpCommandBuilder(username, password)
			.setHost(host )
			.setScheme("https")
			.setPort(port)
			.setAction("delete")
			.setTarget("client")
			.addPathParameter(clientname )
			.setParameter("clientname", clientname)
			.build();
	    HttpResponse response = cmd.execute();
	    if(response.getStatusLine().getStatusCode() == 204) {
	    	KeystoreHelper helper = new KeystoreHelper(keystorename, keystore_pass);
	    	helper.deleteCertificate(clientname);
	    } else if(response.getStatusLine().getStatusCode() == 404) {
	    	throw new RuntimeException("Client does not exist.");
	    }
	}
}
