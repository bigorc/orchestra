package org.oc.orchestra.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Configuration extends Resource {
	private String state = "configured";
	private boolean multiValue = false;
	
	public Configuration(boolean multiValue) {
		this.multiValue = multiValue;
	}
	
	public Configuration() {}
	
	public boolean allowsMultiValue() {
		return multiValue;
	}

	public void setMultiValue(boolean multi) {
		this.multiValue = multi;
	}

	public abstract boolean containsKey(String key);

	public abstract boolean contains(String key, String value);
	public abstract void add(String key, String value);
	public abstract void remove(String name, String value);

	public abstract void init();

	@Override
	public int run(String state) {
		throw new UnsupportedOperationException();
	}
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

}
