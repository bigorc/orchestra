package org.oc.orchestra.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
	String host = "localhost";
	int port = 8183;
	private String username;
	private String password;
	private String zk_connect_string = "localhost:2181";
	private CommandLine cmd;
	private Map<String, String> argValue = new HashMap<String, String>();
	
	public void handle(String[] args) throws ParseException, IOException, 
			org.apache.commons.cli.ParseException {
		Options options = getOptions();
		CommandLineParser parser = new GnuParser();
		cmd = parser.parse(options, args);
        
	    if(cmd.hasOption('u')) {
        	username = cmd.getOptionValue('u');
        } else {
        	username = prompt("username:");
        }
        
        if(cmd.hasOption('p')) {
        	password = cmd.getOptionValue('p');
        } else {
        	password = prompt("password:");
        }
	    
		String [] targets = cmd.getArgs();
		HttpCommandBuilder commandBuilder = new HttpCommandBuilder(username, password);
		host = cmd.hasOption('s') ? cmd.getOptionValue('s') : host;
		port = cmd.hasOption("port") ? Integer.valueOf(cmd.getOptionValue("port")) : port;
		commandBuilder.setScheme("https").setHost(host).setPort(port);
		if(targets.length == 0) {
			startCli();
			System.exit(0);
		} else {
			new TargetFactory(commandBuilder).getTarget(targets[0]).execute(targets[1], cmd);
		}
		
		if(cmd.hasOption("U")) {
			
		}
		
	}

	private static Options getOptions() {
		Options options = new Options();

		Option user = OptionBuilder.withArgName( "User Name" )
                .hasArg()
                .withDescription("User name")
                .withLongOpt("user")
                .create('u');
		Option password = OptionBuilder.withArgName( "password" )
                .hasArg()
                .withDescription("password")
                .withLongOpt("password")
                .create('p');
		Option server = OptionBuilder.withArgName( "server" )
                .hasArg()
                .withDescription("orchestra server")
                .withLongOpt("server")
                .create('s');
		Option port = OptionBuilder.withArgName("port")
				.hasArg()
				.withDescription("Orchestra server port")
				.withLongOpt("port")
				.create("port");
		Option zookeeper = OptionBuilder.withArgName( "zk_connect_string" )
                .hasArg()
                .withDescription("Zookeeper Connection String")
                .withLongOpt("zk_connect_string")
                .create('z');
		Option keystore = OptionBuilder.withArgName( "keystore" )
                .hasArg()
                .withDescription("Keystore Name")
                .withLongOpt("keystore")
                .create('k');
		Option keystore_password = OptionBuilder.withArgName( "keystore_password" )
                .hasArg()
                .withDescription("Keystore Password")
                .withLongOpt("keystore_password")
                .create("kp");
		Option name = OptionBuilder.withArgName( "name" )
                .hasArg()
                .withDescription("name")
                .withLongOpt("name")
                .create('n');
		Option user_pass = OptionBuilder.withArgName( "User Password" )
                .hasArg()
                .withDescription("user password")
                .withLongOpt("user_pass")
                .create("up");
		Option path = OptionBuilder.withArgName( "Path" )
                .hasArg()
                .withDescription("path")
                .withLongOpt("path")
                .create("path");
		options.addOption(user);
		options.addOption(password);
		options.addOption(server);
		options.addOption(port);
		options.addOption(zookeeper);
		options.addOption(keystore);
		options.addOption(keystore_password);
		options.addOption(name);
		options.addOption(user_pass);
		options.addOption(path);
		return options;
	}


	private static String prompt(String output) {
		System.out.print(output);
		Scanner input = new Scanner(System.in, "utf-8");
		
		return input.nextLine();
	}

	private void startCli() {
		String rule = prompt(">");
		while(!rule.equals("exit")) {
			System.out.println(rule);
			Constraint cons = ConstraintParser.parse(rule + "\n");
			cons.enforce();
			rule = prompt(">");
		}
	}

	public static void usage() {
		System.out.println("Usage:");
		System.exit(1);
	}

	class Arg {
		public Arg(String argName, String shortArg) {
			this.argName = argName;
			this.shortArg = shortArg;
		}
		String argName;
		String shortArg;
	}

}
