package org.oc.orchestra.sm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
	private final Logger logger = LoggerFactory.getLogger(Runner.class);
	private static final String sm_path = "sm/";
	private List<String> command = new ArrayList<String>();
//	private String args;
	private Map<String, String> env;
	private String workDir;
	private Process process;
	private String filename;
	
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
		StringBuffer cb = new StringBuffer();
		for(String c : cmd) {
			System.out.println(c);
		}
//		logger.info(cb.toString());
		ProcessBuilder pb = new ProcessBuilder(cmd);
		if(env != null)	pb.environment().putAll(env);
		if(workDir != null) pb.directory(new File(workDir));
//		pb.redirectErrorStream(true);
		process = pb.start();
//		if(filename != null) {
//			OutputStream output = process.getOutputStream();
//			InputStream input = new FileInputStream(filename);
//			
//			byte[] buffer = new byte[1024];
//		    int bytesRead;
//		    while ((bytesRead = input.read(buffer)) != -1)
//		    {
//		        output.write(buffer, 0, bytesRead);
//		    }
//		    output.flush();
//		}
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
		this.filename = filename;
	}
	
}
