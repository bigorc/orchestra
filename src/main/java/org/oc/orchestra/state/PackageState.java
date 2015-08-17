package org.oc.orchestra.state;

import org.oc.orchestra.resource.GeneralResource;
import org.oc.orchestra.resource.Resource;
import org.oc.util.Command;
import org.oc.util.LocalCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PackageState implements State {
	INSTALLED {

		@Override
		public void apply(Resource resource) throws StateException {
			install((String) ((GeneralResource)resource).get("name"));
		}
		
	},
	NOT_INSTALLED {

		@Override
		public void apply(Resource resource) throws StateException {
			remove((String) ((GeneralResource)resource).get("name"));
		}
		
	};
	
	private static final Logger LOG = LoggerFactory.getLogger(PackageState.class);
	
	public static PackageState install(String name) throws StateException {
		Command cmd = new LocalCommand("sudo apt-get install -y " + name);
		String output = cmd.execute();
		if(isInstalled(name)) {
			return INSTALLED;
		} else {
			LOG.error("Failed to install package " + name);
			throw new StateException(output);
		}
	}
	
	public static PackageState remove(String name) throws StateException {
		Command cmd = new LocalCommand("sudo apt-get autoremove --purge -y " + name);
		String output = cmd.execute();
		if(!isInstalled(name)) {
			return NOT_INSTALLED;
		} else {
			LOG.error("Failed to remove package " + name);
			throw new StateException(output);
		}
	}
	
	public static boolean isInstalled(String name) {
		Command cmd = new LocalCommand("sudo aptitude show " + name);
		String output = cmd.execute();
		if(output.contains("State: installed")) {
			return true;
		} else {
			return false;
		}
	}
}