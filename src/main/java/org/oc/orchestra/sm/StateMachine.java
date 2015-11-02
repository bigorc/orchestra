package org.oc.orchestra.sm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachine extends AbstractStateMachine {
	private final Logger logger = LoggerFactory.getLogger(StateMachine.class);
	public static String sm_path = "sm/";
	private Runner runner;
	
	public StateMachine(String sm, List<String> args, Map<String, String> properties) {
		this(sm, args);
		runner.setEnv(properties);
	}
	
	public StateMachine(String sm, List<String> args) {
		String command = new File(sm_path).getAbsolutePath() + "/" + sm;
		this.runner = new Runner(command, args, null, sm_path);
	}

	@Override
	public int run(String state) {
		int exit = 0;
		try {
			exit = runner.run(state);
			if(exit != 0) {
				BufferedReader out = runner.getSystemOut();
				String line;
				while((line = out.readLine()) != null) {
					logger.info(line);
				}
				out = runner.getSystemError();
				while((line = out.readLine()) != null) {
					logger.error(line);
				}
			} else {
				BufferedReader out = runner.getSystemOut();
				String line;
				while((line = out.readLine()) != null) {
					logger.info(line);
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return exit;
	}
	
	@Override
	public String getCurrentState() {
		String state = null;
		String line = null;
		try {
			
			int exit = runner.run("status");
			if(exit != 0) {
				BufferedReader out = runner.getSystemError();
				while((line = out.readLine()) != null) {
					logger.error(line);
				}
			} else {
				BufferedReader out = runner.getSystemOut();
				while((line = out.readLine()) != null) {
					state = line;
					logger.info(line);
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return state;
	}

	@Override
	public void start() {
		run("start");
	}

	@Override
	public String uri() {
		try {
			runner.run("uri");
			BufferedReader out = runner.getSystemOut();
			return out.readLine();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
