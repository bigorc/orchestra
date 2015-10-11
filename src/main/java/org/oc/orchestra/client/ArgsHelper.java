package org.oc.orchestra.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.http.ParseException;
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
        Properties conf = new Properties();
        InputStream is = new FileInputStream("conf/client.conf");
		conf.load(is);
	    if(cmd.hasOption('u')) {
        	username = cmd.getOptionValue('u');
        } else {
        	username = conf.containsKey("username") ? 
        			conf.getProperty("username") : prompt("username:");
        }
        Client.setUsername(username);
        
        if(cmd.hasOption('p')) {
        	password = cmd.getOptionValue('p');
        } else {
        	password = conf.containsKey("password") ?
        			conf.getProperty("password") : prompt("password:");
        }
	    Client.setPassword(password);
	    
		String [] targets = cmd.getArgs();
		HttpCommandBuilder commandBuilder = new HttpCommandBuilder(username, password);
		host = cmd.hasOption('s') ? 
				cmd.getOptionValue('s') : conf.containsKey("server") ? 
						conf.getProperty("server") : host;
		Client.setServer(host);
		
		port = cmd.hasOption("port") ? 
				Integer.valueOf(cmd.getOptionValue("port")) : 
					conf.containsKey("port") ? Integer.valueOf(conf.getProperty("port")) : port;
		Client.setServer_port(port);
		
		zk_connect_string = cmd.hasOption('z') ? cmd.getOptionValue('z') : 
			conf.containsKey("zookeeper.connectString") ? conf.getProperty("zookeeper.connectString") : zk_connect_string;
		Client.setConnectString(zk_connect_string);
		
		if(targets.length == 0) {
			startCli();
			System.exit(0);
		} else {
			commandBuilder.setScheme("https").setHost(host).setPort(port);
			new TargetFactory(commandBuilder).getTarget(targets[0]).execute(targets[1], cmd);
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
		Option recursive = OptionBuilder.withArgName( "Recursive" )
                .withDescription("Recursive")
                .withLongOpt("recursive")
                .create('r');
		Option permission = OptionBuilder.withArgName("Permission")
				.withDescription("Permission")
				.withLongOpt("permission")
				.create("perm");
		permission.setArgs(10);
		
		options.addOption(user);
		options.addOption(password);
		options.addOption(server);
		options.addOption(port);
		options.addOption(zookeeper);
		options.addOption(name);
		options.addOption(user_pass);
		options.addOption(path);
		options.addOption(recursive);
		options.addOption(permission);
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
