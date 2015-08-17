package org.oc.util;

public class CommandBuilderSingleton {
	private final static LocalCommandBuilder LOCAL_COMMAND_BUILDER = new LocalCommandBuilder();
	
	public static CommandFactory getInstance(CommandType type) {
		switch(type) {
		case LOCAL:
			return LOCAL_COMMAND_BUILDER;
		default:
			return LOCAL_COMMAND_BUILDER;
		}
	}
}
