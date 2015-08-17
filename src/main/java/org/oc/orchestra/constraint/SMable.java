package org.oc.orchestra.constraint;

import java.util.List;

public interface SMable {
	public void setSM(String sm);
	
	public String getSM();
	
	public void setState(String state);
	
	public String getState();
	
	public abstract void setArgs(List<String> name);

	public abstract List<String> getArgs();
}
