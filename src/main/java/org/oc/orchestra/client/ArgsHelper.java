package org.oc.orchestra.client;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONValue;
import org.oc.orchestra.constraint.Constraint;
import org.oc.orchestra.parser.ConstraintParser;

public class ArgsHelper {
	static String host = "orchestra";
	static int port = 8183;
	
	public static void handle(String[] args) throws ParseException, IOException, 
			org.apache.commons.cli.ParseException {
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
			String filename = cmd.getOptionValue("U");
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


	private static String prompt(String output) {
		System.out.print(output);
		Scanner input = new Scanner(System.in, "utf-8");
		
		return input.nextLine();
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



}
