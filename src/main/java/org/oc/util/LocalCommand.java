package org.oc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.oc.orchestra.state.PackageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCommand implements Command {
	private static final Logger LOG = LoggerFactory.getLogger(LocalCommand.class);
	private String command;
	public LocalCommand(String command) {
		this.command = command;
	}
	
	public String execute() {
		StringBuffer output = new StringBuffer();
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(this.command);
			
			p.waitFor();
			
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = b.readLine()) != null) {
				output.append(line).append("\n");
			}

			b.close();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.debug(output.toString());
		return output.toString();
	}
}
