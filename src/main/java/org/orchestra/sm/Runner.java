package org.orchestra.sm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
	private final Logger logger = LoggerFactory.getLogger(Runner.class);
	private List<String> command = new ArrayList<String>();
	private Map<String, String> env;
	private String workDir;
	private Process process;
	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public Runner(String command, List<String> args) {
		this.command.add(command);
		if(args != null) this.command.addAll(args);
	}
	
	public Runner(String command, List<String> args, Map<String, String> env) {
		this.command.add(command);
		if(args != null) this.command.addAll(args);
		this.env = env;
	}

	public Runner(String command, List<String> args, Map<String, String> env, String workDir) {
		this.command.add(command);
		if(args != null) this.command.addAll(args);
		this.env = env;
		this.workDir = workDir;
	}
	
	public int run(String arg) throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<String>(command);
		if(arg != null) cmd.add(arg);
		new StringBuffer();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		if(env != null)	pb.environment().putAll(env);
		if(workDir != null) pb.directory(new File(workDir));
		logger.debug("Environment variables:");
		for(Entry<String, String> e : pb.environment().entrySet()) {
			logger.debug(e.getKey() + "=" + e.getValue());
		}
		process = pb.start();

		return process.waitFor();
	}
	
	public InputStream getInputStream() {
		return process.getInputStream();
	}

	public BufferedReader getSystemOut() {
		BufferedReader input = 
				new BufferedReader(new InputStreamReader(process.getInputStream()));
		return input;
	}
	
	public BufferedReader getSystemError() {
		BufferedReader error = 
				new BufferedReader(new InputStreamReader(process.getErrorStream()));
		return error;
	}

	public void setStandardInput(String filename) {
	}
	
}
