package org.oc.util;

public class LocalCommandBuilder extends CommandFactory {

	@Override
	public Command buildCommand(String command) {
		return new LocalCommand(command);
	}

}
